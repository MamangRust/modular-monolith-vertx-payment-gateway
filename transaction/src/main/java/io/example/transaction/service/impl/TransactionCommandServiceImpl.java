package io.example.transaction.service.impl;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.ConflictException;
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

  private Future<MerchantCardContext> markTransactionFailedAndFail(MerchantCardContext ctx, Throwable err) {
    if (ctx.transaction == null) {
      return Future.failedFuture(err);
    }
    return repo.updateTransactionStatus(ctx.transaction.getId(), "failed")
        .recover(statusErr -> {
          logger.error("Failed to mark transaction {} as failed", ctx.transaction.getId(), statusErr);
          return Future.succeededFuture(ctx.transaction);
        })
        .compose(ignored -> Future.<MerchantCardContext>failedFuture(err));
  }

  private boolean matchesIdempotentRequest(Transaction existing, CreateTransactionRequest req) {
    return Objects.equals(existing.getCardNumber(), req.getCardNumber())
        && Objects.equals(existing.getAmount(), (long) req.getAmount())
        && Objects.equals(existing.getPaymentMethod(), req.getPaymentMethod());
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
    String idempotencyKey = req.getIdempotencyKey();
    Future<Transaction> idempotencyCheck = idempotencyKey != null && !idempotencyKey.isBlank()
        ? repo.findByIdempotencyKey(idempotencyKey)
        : Future.succeededFuture();

    return idempotencyCheck
        .compose(existing -> repoMerchant.getMerchantByApiKey(req.getApiKey())
            .map(merchantResp -> {
              span.addEvent("merchant_found");
              ctx.merchant = merchantResp.getData();
              return existing;
            }))
        .compose(existing -> {
          if (existing != null) {
            if (!matchesIdempotentRequest(existing, req)
                || !Objects.equals(existing.getMerchantId(), ctx.merchant.getId())) {
              return Future.failedFuture(new ConflictException(
                  "Idempotency key was already used for a different transaction"));
            }
            ctx.transaction = existing;
            ctx.replayed = true;
            span.addEvent("idempotent_replay");
            span.setAttribute("transaction.id", (long) existing.getId());
            logger.info("Replaying existing transaction {} for idempotency key {}", existing.getId(), idempotencyKey);
            return Future.succeededFuture(ctx);
          }

          return repoCard.getUserCardByCardNumber(req.getCardNumber())
              .map((CardWithEmailResponse cardResp) -> {
                span.addEvent("card_found");
                ctx.userCard = cardResp;
                return ctx;
              });
        })
        .compose(c -> {
          if (c.replayed) {
            return Future.succeededFuture(c);
          }
          // Reserve the idempotency key before any balance mutation. The unique
          // index plus this lookup closes the concurrent retry window.
          span.addEvent("transaction_create_start");
          CreateTransactionRequest repoReq = req.toBuilder().setMerchantId(ctx.merchant.getId()).build();
          return repo.createTransaction(repoReq).compose(created -> {
            if (created != null) {
              ctx.transaction = created;
              return Future.succeededFuture(ctx);
            }
            if (idempotencyKey == null || idempotencyKey.isBlank()) {
              return Future.<MerchantCardContext>failedFuture(
                  new IllegalStateException("Transaction insert returned no row"));
            }
            return repo.findByIdempotencyKey(idempotencyKey).compose(raced -> {
              if (raced == null) {
                return Future.<MerchantCardContext>failedFuture(
                    new IllegalStateException("Transaction reservation was lost"));
              }
              if (!matchesIdempotentRequest(raced, req)
                  || !Objects.equals(raced.getMerchantId(), ctx.merchant.getId())) {
                return Future.failedFuture(new ConflictException(
                    "Idempotency key was already used for a different transaction"));
              }
              ctx.transaction = raced;
              ctx.replayed = true;
              return Future.succeededFuture(ctx);
            });
          });
        })
        .compose(c -> {
          if (c.replayed) {
            return Future.succeededFuture(c);
          }
          return repoSaldo.getSaldoByCardNumber(req.getCardNumber())
              .compose((ApiResponseSaldo saldoResp) -> {
                int currentBalance = saldoResp.getData().getTotalBalance();
                span.addEvent("saldo_found");

                if (currentBalance < req.getAmount()) {
                  span.addEvent("insufficient_balance");
                  return markTransactionFailedAndFail(c,
                      new BadRequestException("Insufficient balance for card: " + req.getCardNumber()));
                }

                ctx.originalBalance = currentBalance;
                span.addEvent("saldo_deduct_start");
                return repoSaldo.updateSaldoDelta(req.getCardNumber(), -req.getAmount())
                    .map(v -> {
                      span.addEvent("saldo_deducted");
                      return c;
                    });
              })
              .recover(err -> markTransactionFailedAndFail(c, err));
        })
        .compose(c -> {
          if (c.replayed) {
            return Future.succeededFuture(c);
          }
          span.addEvent("transaction_mark_success");
          return repo.updateTransactionStatus(c.transaction.getId(), "success")
              .compose(status -> {
                span.addEvent("merchant_card_fetch_start");
                return repoCard.getCardByUserId(c.merchant.getUserId())
                    .map(cardResp -> {
                      c.merchantCard = cardResp.getData();
                      return c;
                    });
              })
              .compose(updated -> repoSaldo.updateSaldoDelta(updated.merchantCard.getCardNumber(), req.getAmount())
                  .map(v -> updated))
              .recover(err -> compensateTransactionDebitAndFail(c, req, err));
        })
        .compose(c -> {
          if (c.replayed) {
            return Future.succeededFuture(c);
          }
          return sendTransactionEmail(c.userCard.getEmail(), req.getAmount(), c.transaction.getId())
              .map(v -> c);
        })
        .map(c -> TransactionResponse.from(c.transaction))
        .onSuccess(
            v -> tracingMetrics.completeSpanSuccess(tracingContext, "create", "Transaction created successfully"))
        .onFailure(err -> {
          logger.error("Failed to create transaction for card: {}", req.getCardNumber(), err);
          tracingMetrics.completeSpanError(tracingContext, "create", err.getMessage());
        });
  }

  private Future<MerchantCardContext> compensateTransactionDebitAndFail(
      MerchantCardContext ctx, CreateTransactionRequest req, Throwable err) {
    return repoSaldo.updateSaldoDelta(req.getCardNumber(), req.getAmount())
        .recover(compensationErr -> {
          logger.error("Failed to compensate transaction debit {}", ctx.transaction.getId(), compensationErr);
          return Future.succeededFuture();
        })
        .compose(ignored -> repo.updateTransactionStatus(ctx.transaction.getId(), "failed")
            .recover(statusErr -> Future.succeededFuture(ctx.transaction))
            .compose(ignoredStatus -> Future.<MerchantCardContext>failedFuture(err)));
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
    boolean replayed;
    pb.card.Card.CardResponse merchantCard;
  }

  private static class UpdateTransactionContext {
    pb.merchant.Merchant.MerchantResponse merchant;
    Transaction existing;
    pb.card.Card.CardResponse card;
    Transaction updated;
  }
}