package io.example.transaction.service.impl;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.ForbiddenException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.example.common.utils.EmailTemplate;
import io.example.transaction.model.Transaction;
import io.example.transaction.model.TransactionResponse;
import io.example.transaction.model.TransactionResponseDeleteAt;
import io.example.transaction.repository.CardClientRepository;
import io.example.transaction.repository.MerchantClientRepository;
import io.example.transaction.repository.SaldoClientRepository;
import io.example.transaction.repository.TransactionCommandRepository;
import io.example.transaction.repository.TransactionQueryRepository;
import io.example.transaction.service.TransactionCommandService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;
import pb.card.Card.CardWithEmailResponse;
import pb.saldo.Saldo.ApiResponseSaldo;
import pb.transaction.TransactionCommand.CreateTransactionRequest;
import pb.transaction.TransactionCommand.UpdateTransactionRequest;

@RequiredArgsConstructor
public class TransactionCommandServiceImpl implements TransactionCommandService {
  private static final Logger logger = LoggerFactory.getLogger(TransactionCommandServiceImpl.class);
  private static final String CACHE_PREFIX = "transaction:";

  private final TransactionCommandRepository repo;
  private final TransactionQueryRepository queryRepository;
  private final MerchantClientRepository repoMerchant;
  private final CardClientRepository repoCard;
  private final SaldoClientRepository repoSaldo;
  private final RedisService redisService;
  private final KafkaService kafkaService;
  private final TracingMetrics tracingMetrics;

  private Future<Void> invalidateCache(Integer id) {
    return redisService.delete(CACHE_PREFIX + id)
        .compose(v -> redisService.delete(CACHE_PREFIX + "list:*"))
        .<Void>mapEmpty();
  }

  private Future<Void> invalidateListCache() {
    return redisService.delete(CACHE_PREFIX + "list:*").<Void>mapEmpty();
  }

  private Future<Void> sendTransactionEmail(String email, int amount, Integer transactionId) {
    if (kafkaService == null || email == null || email.isEmpty()) {
      return Future.succeededFuture();
    }

    String htmlBody = EmailTemplate.generateHtml(Map.of(
        "Title", "Transaction Successful",
        "Message", String.format("Your transaction of %d has been processed successfully.", amount),
        "Button", "View History",
        "Link", "https://sanedge.example.com/transaction/history"));

    JsonObject emailPayload = new JsonObject()
        .put("email", email)
        .put("subject", "Transaction Successful - SanEdge")
        .put("body", htmlBody);

    return kafkaService
        .sendMessage("email-service-topic-transaction-create", String.valueOf(transactionId), emailPayload)
        .<Void>mapEmpty()
        .onFailure(
            err -> logger.error("Failed to send transaction email via Kafka for transactionId: {}", transactionId, err))
        .recover(err -> Future.succeededFuture());
  }

  @Override
  public Future<TransactionResponse> createTransaction(CreateTransactionRequest req) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TransactionCommandService.createTransaction",
        Attributes.builder()
            .put("transaction.card_number", Objects.requireNonNull(req.getCardNumber()))
            .put("transaction.amount", (long) req.getAmount())
            .build());

    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));
    logger.info("Creating transaction for card: {}, amount: {}", req.getCardNumber(), req.getAmount());

    MerchantCardContext ctx = new MerchantCardContext();

    return repoMerchant.getMerchantByApiKey(req.getApiKey())
        .map(merchantResp -> {
          span.addEvent("merchant_found");
          ctx.merchant = merchantResp.getData();
          return ctx;
        })
        .compose(c -> repoCard.getUserCardByCardNumber(req.getCardNumber())
            .map((CardWithEmailResponse cardResp) -> {
              span.addEvent("card_found");
              ctx.userCard = cardResp;
              return c;
            }))
        .compose(c -> repoSaldo.getSaldoByCardNumber(req.getCardNumber())
            .compose((ApiResponseSaldo saldoResp) -> {
              int currentBalance = saldoResp.getData().getTotalBalance();
              span.addEvent("saldo_found");

              if (currentBalance < req.getAmount()) {
                span.addEvent("insufficient_balance");
                return Future.<MerchantCardContext>failedFuture(
                    new BadRequestException("Insufficient balance for card: " + req.getCardNumber()));
              }

              ctx.originalBalance = currentBalance;
              int newBalance = currentBalance - req.getAmount();
              span.addEvent("saldo_deduct_start");

              return repoSaldo.updateSaldoBalance(req.getCardNumber(), newBalance)
                  .map(v -> {
                    span.addEvent("saldo_deducted");
                    return c;
                  });
            }))
        .compose(c -> {
          span.addEvent("transaction_create_start");
          CreateTransactionRequest repoReq = req.toBuilder().setMerchantId(ctx.merchant.getId()).build();

          return repo.createTransaction(repoReq)
              .recover(err -> {
                span.recordException(err != null ? err : new RuntimeException("Unknown error"));
                span.addEvent("transaction_create_failed_rolling_back");
                return repoSaldo.updateSaldoBalance(req.getCardNumber(), ctx.originalBalance)
                    .compose(v -> {
                      span.addEvent("saldo_rollback_success");
                      if (ctx.transaction != null) {
                        return repo.updateTransactionStatus(ctx.transaction.getId(), "failed")
                            .compose(x -> Future.<Transaction>failedFuture(err));
                      }
                      return Future.<Transaction>failedFuture(err);
                    })
                    .recover(rollbackErr -> {
                      logger.error("Failed to rollback saldo for card: {}", req.getCardNumber(), rollbackErr);
                      return Future.<Transaction>failedFuture(err);
                    });
              });
        })
        .compose(transaction -> {
          span.addEvent("transaction_created");
          ctx.transaction = transaction;
          span.addEvent("transaction_mark_success");
          return repo.updateTransactionStatus(transaction.getId(), "success").map(v -> transaction);
        })
        .compose(transaction -> {
          span.addEvent("merchant_card_fetch_start");
          return repoCard.getCardByUserId(ctx.merchant.getUserId())
              .map(cardResp -> {
                ctx.merchantCard = cardResp.getData();
                return transaction;
              });
        })
        .compose(transaction -> repoSaldo.getSaldoByCardNumber(ctx.merchantCard.getCardNumber())
            .compose(merchantSaldoResp -> {
              int newMerchantBalance = merchantSaldoResp.getData().getTotalBalance() + req.getAmount();
              return repoSaldo.updateSaldoBalance(ctx.merchantCard.getCardNumber(), newMerchantBalance)
                  .map(v -> transaction);
            }))
        .compose(transaction -> sendTransactionEmail(ctx.userCard.getEmail(), req.getAmount(), transaction.getId())
            .map(v -> transaction))
        .map(TransactionResponse::from)
        .onSuccess(
            v -> tracingMetrics.completeSpanSuccess(tracingContext, "create", "Transaction created successfully"))
        .onFailure(err -> {
          logger.error("Failed to create transaction for card: {}", req.getCardNumber(), err);
          tracingMetrics.completeSpanError(tracingContext, "create", err.getMessage());
        });
  }

  @Override
  public Future<TransactionResponse> updateTransaction(UpdateTransactionRequest req) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TransactionCommandService.updateTransaction",
        Attributes.builder()
            .put("transaction.id", (long) req.getTransactionId())
            .put("transaction.amount", (long) req.getAmount())
            .build());

    logger.info("Updating transaction: {}, amount: {}", req.getTransactionId(), req.getAmount());
    UpdateTransactionContext ctx = new UpdateTransactionContext();

    return repo.getTransactionById(req.getTransactionId())
        .compose(existing -> {
          if (existing == null) {
            // ✅ Fix 1: Type witness harus cocok dengan return type getMerchantByApiKey
            return repoMerchant.getMerchantByApiKey(req.getApiKey())
                .<Transaction>map(merchantResp -> {
                  ctx.existing = existing;
                  ctx.merchant = merchantResp.getData();
                  return existing; // placeholder, akan di-override di chain berikutnya
                });
          }
          ctx.existing = existing;
          return repoMerchant.getMerchantByApiKey(req.getApiKey())
              .map(merchantResp -> {
                ctx.merchant = merchantResp.getData();
                return existing;
              });
        })
        .compose(existing -> {
          if (ctx.existing.getMerchantId() != ctx.merchant.getId()) {
            return Future.<Transaction>failedFuture(
                new ForbiddenException("Transaction does not belong to this merchant"));
          }
          return Future.succeededFuture(existing);
        })
        .compose(existing -> repoCard.getCardByCardNumber(ctx.existing.getCardNumber())
            .map(cardResp -> {
              ctx.card = cardResp.getData();
              return existing;
            }))
        .compose(existing -> repoSaldo.getSaldoByCardNumber(ctx.card.getCardNumber())
            .compose(saldoResp -> {
              int currentBalance = saldoResp.getData().getTotalBalance();
              long restoredBalance = (long) currentBalance + ctx.existing.getAmount();

              if (restoredBalance < req.getAmount()) {
                // ✅ Fix 2: <Transaction> bukan <Void>, agar cocok dengan branch sukses
                return Future.<Transaction>failedFuture(
                    new BadRequestException("Insufficient balance"));
              }

              long newBalance = restoredBalance - req.getAmount();
              UpdateTransactionRequest repoReq = req.toBuilder()
                  .setMerchantId(ctx.merchant.getId())
                  .build();

              return repoSaldo.updateSaldoBalance(ctx.card.getCardNumber(), (int) newBalance)
                  .compose(v2 -> repo.updateTransaction(repoReq))
                  .compose(updated -> {
                    ctx.updated = updated;
                    return repo.updateTransactionStatus(req.getTransactionId(), "success")
                        .map(v3 -> updated);
                  });
            }))
        .compose(updated -> invalidateCache(req.getTransactionId()).map(v -> updated))
        .map(TransactionResponse::from)
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "update",
            "Transaction updated successfully"))
        .onFailure(err -> {
          logger.error("Failed to update transaction: {}", req.getTransactionId(), err);
          tracingMetrics.completeSpanError(tracingContext, "update", err.getMessage());
        });
  }

  @Override
  public Future<TransactionResponseDeleteAt> trashTransaction(Integer transactionId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TransactionCommandService.trashTransaction",
        Attributes.builder().put("transaction.id", (long) transactionId).build());

    return repo.trashed(transactionId)
        .compose(transaction -> {
          if (transaction == null) {
            return Future
                .<Transaction>failedFuture(new NotFoundException("Transaction not found with id: " + transactionId));
          }
          return invalidateCache(transactionId).map(v -> transaction);
        })
        .map(TransactionResponseDeleteAt::from)
        .onSuccess(
            v -> tracingMetrics.completeSpanSuccess(tracingContext, "trashed", "Transaction trashed successfully"))
        .onFailure(err -> {
          logger.error("Failed to trash transaction: {}", transactionId, err);
          tracingMetrics.completeSpanError(tracingContext, "trashed", err.getMessage());
        });
  }

  @Override
  public Future<TransactionResponseDeleteAt> restoreTransaction(Integer transactionId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TransactionCommandService.restoreTransaction",
        Attributes.builder().put("transaction.id", (long) transactionId).build());

    return queryRepository.findByTrashed(transactionId)
        .compose(trashed -> {
          if (trashed == null)
            return Future.failedFuture(new BadRequestException("Transaction not found or must be trashed first"));
          return repo.restoreTransaction(transactionId);
        })
        .compose(transaction -> {
          if (transaction == null) {
            return Future
                .<Transaction>failedFuture(new NotFoundException("Transaction not found with id: " + transactionId));
          }
          return invalidateCache(transactionId).<Transaction>map(v -> transaction);
        })
        .map(TransactionResponseDeleteAt::from)
        .onSuccess(
            v -> tracingMetrics.completeSpanSuccess(tracingContext, "restore", "Transaction restored successfully"))
        .onFailure(err -> {
          logger.error("Failed to restore transaction: {}", transactionId, err);
          tracingMetrics.completeSpanError(tracingContext, "restore", err.getMessage());
        });
  }

  @Override
  public Future<Void> deleteTransactionPermanently(Integer transactionId) {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("TransactionService.deletePermanent",
        Attributes.builder().put("transaction.id", (long) transactionId).build());

    return queryRepository.findByTrashed(transactionId)
        .compose(trashed -> {
          if (trashed == null) {
            return Future.<Void>failedFuture(
                new BadRequestException("Transaction not found or must be trashed first"));
          }
          return repo.deletePermanently(transactionId)
              .compose(deleted -> {
                if (!deleted) {
                  return Future.<Void>failedFuture(
                      new BadRequestException("Transaction not found or must be trashed first"));
                }
                return invalidateCache(transactionId);
              });
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx,
            "deletePermanent", "Transaction deleted permanently"))
        .onFailure(err -> {
          logger.error("Failed to deletePermanent transaction: {}", transactionId, err);
          tracingMetrics.completeSpanError(ctx, "deletePermanent", err.getMessage());
        });
  }

  @Override
  public Future<Void> restoreAllTransactions() {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("TransactionService.restoreAll");

    return repo.restoreAllTransactions()
        .compose(count -> {
          if (count == 0) {
            return Future.<Void>failedFuture(new NotFoundException("No trashed transactions found"));
          }
          return invalidateListCache();
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "restore_all", "All transactions restored"))
        .onFailure(err -> {
          logger.error("Failed to restore all transactions", err);
          tracingMetrics.completeSpanError(ctx, "restore_all", err.getMessage());
        });
  }

  @Override
  public Future<Void> deleteAllPermanentTransactions() {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("TransactionService.deleteAllPermanent");

    return repo.deleteAllPermanentTransactions()
        .compose(count -> {
          if (count == 0) {
            return Future.<Void>failedFuture(new NotFoundException("No trashed transactions found"));
          }
          return invalidateListCache();
        })
        .onSuccess(
            v -> tracingMetrics.completeSpanSuccess(ctx, "deleteAllPermanent", "All transactions permanently deleted"))
        .onFailure(err -> {
          logger.error("Failed to permanently delete all transactions", err);
          tracingMetrics.completeSpanError(ctx, "deleteAllPermanent", err.getMessage());
        });
  }

  private static class MerchantCardContext {
    pb.merchant.Merchant.MerchantResponse merchant;
    pb.card.Card.CardWithEmailResponse userCard;
    int originalBalance;
    Transaction transaction;
    pb.card.Card.CardResponse merchantCard;
  }

  private static class UpdateTransactionContext {
    pb.merchant.Merchant.MerchantResponse merchant;
    Transaction existing;
    pb.card.Card.CardResponse card;
    Transaction updated;
  }
}