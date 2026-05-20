package io.example.transaction.service.impl;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.exception.BadRequestException;
import io.example.common.exception.ForbiddenException;
import io.example.common.exception.NotFoundException;
import io.example.common.model.ApiResponse;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.example.transaction.model.Transaction;
import io.example.transaction.model.TransactionResponse;
import io.example.transaction.model.TransactionResponseDeleteAt;
import io.example.transaction.repository.CardClientRepository;
import io.example.transaction.repository.MerchantClientRepository;
import io.example.transaction.repository.SaldoClientRepository;
import io.example.transaction.repository.TransactionCommandRepository;
import io.example.transaction.service.TransactionCommandService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import pb.card.Card.CardWithEmailResponse;
import pb.saldo.Saldo.ApiResponseSaldo;
import pb.transaction.Transaction.FindByIdTransactionRequest;
import pb.transaction.TransactionCommand.CreateTransactionRequest;
import pb.transaction.TransactionCommand.UpdateTransactionRequest;

public class TransactionCommandServiceImpl implements TransactionCommandService {
  private static final Logger logger = LoggerFactory.getLogger(TransactionCommandServiceImpl.class);

  private final TransactionCommandRepository repo;
  private final MerchantClientRepository repoMerchant;
  private final CardClientRepository repoCard;
  private final SaldoClientRepository repoSaldo;
  private final RedisService redisService;
  private final KafkaService kafkaService;
  private final TracingMetrics tracingMetrics;

  private static final String CACHE_PREFIX = "transaction:";

  public TransactionCommandServiceImpl(
      TransactionCommandRepository repo,
      MerchantClientRepository repoMerchant,
      CardClientRepository repoCard,
      SaldoClientRepository repoSaldo,
      RedisService redisService,
      KafkaService kafkaService,
      TracingMetrics tracingMetrics) {
    this.repo = repo;
    this.repoMerchant = repoMerchant;
    this.repoCard = repoCard;
    this.repoSaldo = repoSaldo;
    this.redisService = redisService;
    this.kafkaService = kafkaService;
    this.tracingMetrics = tracingMetrics;
  }

  @Override
  public Future<ApiResponse<TransactionResponse>> createTransaction(CreateTransactionRequest req) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TransactionCommandService.createTransaction",
        Attributes.builder()
            .put("transaction.card_number", Objects.requireNonNull(req.getCardNumber()))
            .put("transaction.amount", (long) req.getAmount())
            .build());

    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    logger.info("Creating transaction for card: {}, amount: {}", req.getCardNumber(), req.getAmount());

    return repoMerchant.getMerchantByApiKey(req.getApiKey())
        .compose(merchantResp -> {
          span.addEvent("merchant_found");
          span.setAttribute("merchant.id", (long) merchantResp.getData().getId());

          return repoCard.getUserCardByCardNumber(req.getCardNumber())
              .map((CardWithEmailResponse cardResp) -> {
                span.addEvent("card_found");
                span.setAttribute("card.id", (long) cardResp.getId());
                return new MerchantCardContext(merchantResp.getData(), cardResp);
              });
        })
        .compose((MerchantCardContext ctx) -> repoSaldo.getSaldoByCardNumber(req.getCardNumber())
            .compose((ApiResponseSaldo saldoResp) -> {
              int currentBalance = saldoResp.getData().getTotalBalance();
              span.addEvent("saldo_found");
              span.setAttribute("saldo.current_balance", (long) currentBalance);

              if (currentBalance < req.getAmount()) {
                span.addEvent("insufficient_balance");
                return Future
                    .failedFuture(new BadRequestException("Insufficient balance for card: " + req.getCardNumber()));
              }

              int newBalance = currentBalance - req.getAmount();
              span.addEvent("saldo_deduct_start");

              return repoSaldo.updateSaldoBalance(req.getCardNumber(), newBalance)
                  .map(v -> {
                    span.addEvent("saldo_deducted");
                    ctx.originalBalance = currentBalance;
                    return ctx;
                  });
            }))
        .compose((MerchantCardContext ctx) -> {
          span.addEvent("transaction_create_start");
          CreateTransactionRequest repoReq = req.toBuilder()
              .setMerchantId(ctx.merchant.getId())
              .build();

          return repo
              .createTransaction(repoReq)
              .map((Transaction transaction) -> {
                span.addEvent("transaction_created");
                span.setAttribute("transaction.id", (long) transaction.getId());
                ctx.transaction = transaction;
                return ctx;
              })
              .recover(err -> {
                span.recordException(err != null ? err : new RuntimeException("Unknown error"));
                span.addEvent("transaction_create_failed_rolling_back");
                return repoSaldo.updateSaldoBalance(req.getCardNumber(), ctx.originalBalance)
                    .compose(v -> {
                      span.addEvent("saldo_rollback_success");
                      if (ctx.transaction != null) {
                        return repo.updateTransactionStatus(ctx.transaction.getId(), "failed")
                            .compose(x -> Future.<MerchantCardContext>failedFuture(err));
                      }
                      return Future.<MerchantCardContext>failedFuture(err);
                    })
                    .recover(rollbackErr -> {
                      logger.error("Failed to rollback saldo for card: {}", req.getCardNumber(), rollbackErr);
                      return Future.<MerchantCardContext>failedFuture(err);
                    });
              });
        })
        .compose((MerchantCardContext ctx) -> {
          span.addEvent("transaction_mark_success");
          return repo.updateTransactionStatus(ctx.transaction.getId(), "success")
              .map(v -> ctx);
        })
        .compose((MerchantCardContext ctx) -> {
          span.addEvent("merchant_card_fetch_start");
          return repoCard.getCardByUserId(ctx.merchant.getUserId())
              .map(cardResp -> {
                ctx.merchantCard = cardResp.getData();
                return ctx;
              });
        })
        .compose((MerchantCardContext ctx) -> repoSaldo.getSaldoByCardNumber(ctx.merchantCard.getCardNumber())
            .compose(merchantSaldoResp -> {
              int newMerchantBalance = merchantSaldoResp.getData().getTotalBalance() + req.getAmount();
              return repoSaldo.updateSaldoBalance(ctx.merchantCard.getCardNumber(), newMerchantBalance)
                  .map(v -> ctx);
            }))
        .compose((MerchantCardContext ctx) -> {
          // Send Kafka notification (Async but in chain)
          if (kafkaService != null && ctx.userCard.getEmail() != null && !ctx.userCard.getEmail().isEmpty()) {
            String htmlBody = io.example.common.utils.EmailTemplate.generateHtml(java.util.Map.of(
                "Title", "Transaction Successful",
                "Message", String.format("Your transaction of %d has been processed successfully.", req.getAmount()),
                "Button", "View History",
                "Link", "https://sanedge.example.com/transaction/history"));

            JsonObject emailPayload = new JsonObject()
                .put("email", ctx.userCard.getEmail())
                .put("subject", "Transaction Successful - SanEdge")
                .put("body", htmlBody);

            return kafkaService.sendMessage("email-service-topic-transaction-create",
                String.valueOf(ctx.transaction.getId()), emailPayload)
                .map(v -> {
                  tracingMetrics.completeSpanSuccess(tracingContext, "create", "Transaction created successfully");
                  return ApiResponse.success("Transaction created successfully", TransactionResponse.from(ctx.transaction));
                })
                .recover(err -> {
                  logger.error("Failed to send transaction email via Kafka for transactionId: {}", ctx.transaction.getId(), err);
                  tracingMetrics.completeSpanSuccess(tracingContext, "create", "Transaction created successfully (email failed)");
                  return Future.succeededFuture(ApiResponse.success("Transaction created successfully", TransactionResponse.from(ctx.transaction)));
                });
          }

          tracingMetrics.completeSpanSuccess(tracingContext, "create", "Transaction created successfully");
          return Future.succeededFuture(ApiResponse.success("Transaction created successfully", TransactionResponse.from(ctx.transaction)));
        })
        .recover(err -> {
          logger.error("Failed to create transaction for card: {}", req.getCardNumber(), err);
          tracingMetrics.completeSpanError(tracingContext, "create", err.getMessage());
          return Future.succeededFuture(ApiResponse.error("Failed to create transaction: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<TransactionResponse>> updateTransaction(UpdateTransactionRequest req) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TransactionCommandService.updateTransaction",
        Attributes.builder()
            .put("transaction.id", (long) req.getTransactionId())
            .put("transaction.amount", (long) req.getAmount())
            .build());

    logger.info("Updating transaction: {}, amount: {}", req.getTransactionId(), req.getAmount());

    FindByIdTransactionRequest findReq = FindByIdTransactionRequest.newBuilder()
        .setTransactionId(req.getTransactionId())
        .build();

    return repo.getTransactionById(findReq)
        .compose((Transaction existing) -> {
          if (existing == null)
            return Future.failedFuture(new NotFoundException("Transaction not found"));

          return repoMerchant.getMerchantByApiKey(req.getApiKey())
              .compose(merchantResp -> {
                if (existing.getMerchantId() != merchantResp.getData().getId()) {
                  return Future.failedFuture(new ForbiddenException("Transaction does not belong to this merchant"));
                }
                return Future.succeededFuture(new UpdateTransactionContext(merchantResp.getData(), existing));
              });
        })
        .compose((UpdateTransactionContext ctx) -> repoCard.getCardByCardNumber(ctx.existing.getCardNumber())
            .map(cardResp -> {
              ctx.card = cardResp.getData();
              return ctx;
            }))
        .compose((UpdateTransactionContext ctx) -> repoSaldo.getSaldoByCardNumber(ctx.card.getCardNumber())
            .compose(saldoResp -> {
              int currentBalance = saldoResp.getData().getTotalBalance();
              long restoredBalance = (long) currentBalance + ctx.existing.getAmount();

              if (restoredBalance < req.getAmount()) {
                return Future.failedFuture(new BadRequestException("Insufficient balance"));
              }

              long newBalance = restoredBalance - req.getAmount();

              UpdateTransactionRequest repoReq = req.toBuilder()
                  .setMerchantId(ctx.merchant.getId())
                  .build();

              return repoSaldo.updateSaldoBalance(ctx.card.getCardNumber(), (int) newBalance)
                  .compose(v -> repo.updateTransaction(repoReq)
                      .map(updated -> {
                        ctx.updated = updated;
                        return ctx;
                      }));
            }))
        .compose((UpdateTransactionContext ctx) -> repo.updateTransactionStatus(req.getTransactionId(), "success")
            .map(v -> ctx))
        .compose((UpdateTransactionContext ctx) -> redisService.delete(CACHE_PREFIX + req.getTransactionId())
            .map(v -> ctx)
            .recover(err -> Future.succeededFuture(ctx)))
        .map((UpdateTransactionContext ctx) -> {
          tracingMetrics.completeSpanSuccess(tracingContext, "update", "Transaction updated successfully");
          return ApiResponse.success("Transaction updated successfully", TransactionResponse.from(ctx.updated));
        })
        .recover(err -> {
          logger.error("Failed to update transaction: {}", req.getTransactionId(), err);
          tracingMetrics.completeSpanError(tracingContext, "update", err.getMessage());
          return Future.succeededFuture(ApiResponse.error("Failed to update transaction: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<TransactionResponseDeleteAt>> trashTransaction(Integer transactionId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TransactionCommandService.trashTransaction",
        Attributes.builder()
            .put("transaction.id", (long) transactionId)
            .build());

    logger.info("Trashing transaction: {}", transactionId);

    FindByIdTransactionRequest findReq = FindByIdTransactionRequest.newBuilder()
        .setTransactionId(transactionId)
        .build();

    return repo.trashTransaction(findReq)
        .compose(transaction -> {
          if (transaction == null) {
            return Future.failedFuture(new NotFoundException("Transaction not found with id: " + transactionId));
          }
          String cacheKey = CACHE_PREFIX + transactionId;
          return redisService.delete(cacheKey).map(v -> transaction);
        })
        .map(transaction -> {
          tracingMetrics.completeSpanSuccess(tracingContext, "trashed", "Transaction trashed successfully");
          return ApiResponse.success("Transaction trashed successfully", TransactionResponseDeleteAt.from(transaction));
        })
        .recover(err -> {
          logger.error("Failed to trash transaction: {}", transactionId, err);
          tracingMetrics.completeSpanError(tracingContext, "trashed", err.getMessage());
          return Future.succeededFuture(ApiResponse.error("Failed to trash transaction: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<TransactionResponseDeleteAt>> restoreTransaction(Integer transactionId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TransactionCommandService.restoreTransaction",
        Attributes.builder()
            .put("transaction.id", (long) transactionId)
            .build());

    logger.info("Restoring transaction: {}", transactionId);

    FindByIdTransactionRequest findReq = FindByIdTransactionRequest.newBuilder()
        .setTransactionId(transactionId)
        .build();

    return repo.restoreTransaction(findReq)
        .compose(transaction -> {
          if (transaction == null) {
            return Future.failedFuture(new NotFoundException("Transaction not found with id: " + transactionId));
          }
          String cacheKey = CACHE_PREFIX + transactionId;
          return redisService.delete(cacheKey).map(v -> transaction);
        })
        .map(transaction -> {
          tracingMetrics.completeSpanSuccess(tracingContext, "restore", "Transaction restored successfully");
          return ApiResponse.success("Transaction restored successfully",
              TransactionResponseDeleteAt.from(transaction));
        })
        .recover(err -> {
          logger.error("Failed to restore transaction: {}", transactionId, err);
          tracingMetrics.completeSpanError(tracingContext, "restore", err.getMessage());
          return Future.succeededFuture(ApiResponse.error("Failed to restore transaction: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<Void>> deleteTransactionPermanently(Integer transactionId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TransactionCommandService.deleteTransactionPermanently",
        Attributes.builder()
            .put("transaction.id", (long) transactionId)
            .build());

    logger.info("Permanently deleting transaction: {}", transactionId);

    FindByIdTransactionRequest findReq = FindByIdTransactionRequest.newBuilder()
        .setTransactionId(transactionId)
        .build();

    return repo.deletePermanently(findReq)
        .compose(v -> {
          String cacheKey = CACHE_PREFIX + transactionId;
          return redisService.delete(cacheKey).mapEmpty();
        })
        .map(v -> {
          tracingMetrics.completeSpanSuccess(tracingContext, "deletePermanent", "Transaction deleted permanently");
          return ApiResponse.<Void>success("success", null);
        })
        .recover(throwable -> {
          logger.error("Failed to deletePermanent transaction: {}", transactionId, throwable);
          tracingMetrics.completeSpanError(tracingContext, "deletePermanent", throwable.getMessage());
          return Future.succeededFuture(ApiResponse.error("Failed to delete transaction: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<Void>> restoreAllTransactions() {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("TransactionCommandService.restoreAll");

    logger.info("Attempting to restore all trashed transactions");

    return repo.restoreAll()
        .compose(v -> {
          tracingMetrics.completeSpanSuccess(tracingContext, "restore_all", "All transactions restored");
          return Future.succeededFuture(ApiResponse.<Void>success("All transactions restored successfully"));
        })
        .recover(throwable -> {
          logger.error("Failed to restore all transactions", throwable);
          tracingMetrics.completeSpanError(tracingContext, "restore_all", throwable.getMessage());
          return Future
              .succeededFuture(
                  ApiResponse.<Void>error("Failed to restore all transactions: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<Void>> deleteAllPermanentTransactions() {
    TracingMetrics.TracingContext tracingContext = tracingMetrics
        .startSpan("TransactionCommandService.deleteAllPermanent");

    logger.info("Attempting to permanently delete all trashed transactions");

    return repo.deleteAllPermanent()
        .compose(v -> {
          tracingMetrics.completeSpanSuccess(tracingContext, "deleteAllPermanent",
              "All transactions permanently deleted");
          return Future.succeededFuture(ApiResponse.<Void>success("All transactions permanently deleted"));
        })
        .recover(throwable -> {
          logger.error("Failed to permanently delete all transactions", throwable);
          tracingMetrics.completeSpanError(tracingContext, "deleteAllPermanent", throwable.getMessage());
          return Future.succeededFuture(
              ApiResponse.<Void>error("Failed to permanently delete all transactions: " + throwable.getMessage()));
        });
  }

  private static class MerchantCardContext {
    final pb.merchant.Merchant.MerchantResponse merchant;
    final pb.card.Card.CardWithEmailResponse userCard;
    int originalBalance;
    Transaction transaction;
    pb.card.Card.CardResponse merchantCard;

    MerchantCardContext(pb.merchant.Merchant.MerchantResponse merchant, pb.card.Card.CardWithEmailResponse userCard) {
      this.merchant = merchant;
      this.userCard = userCard;
    }
  }

  private static class UpdateTransactionContext {
    final pb.merchant.Merchant.MerchantResponse merchant;
    final Transaction existing;
    pb.card.Card.CardResponse card;
    Transaction updated;

    UpdateTransactionContext(pb.merchant.Merchant.MerchantResponse merchant, Transaction existing) {
      this.merchant = merchant;
      this.existing = existing;
    }
  }
}
