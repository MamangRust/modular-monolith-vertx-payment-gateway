package io.example.withdraw.service.impl;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.ConflictException;
import io.example.common.exception.grpc.InsufficientBalanceException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.example.common.utils.EmailTemplate;
import io.example.withdraw.domain.requests.CreateWithdrawRequest;
import io.example.withdraw.domain.requests.UpdateWithdrawRequest;
import io.example.withdraw.domain.requests.UpdateWithdrawStatus;
import io.example.withdraw.model.Withdraw;
import io.example.withdraw.model.WithdrawResponse;
import io.example.withdraw.model.WithdrawResponseDeleteAt;
import io.example.withdraw.repository.CardClientRepository;
import io.example.withdraw.repository.SaldoClientRepository;
import io.example.withdraw.repository.WithdrawCommandRepository;
import io.example.withdraw.repository.WithdrawQueryRepository;
import io.example.withdraw.service.WithdrawCommandService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WithdrawCommandServiceImpl implements WithdrawCommandService {

  private static final Logger logger = LoggerFactory.getLogger(WithdrawCommandServiceImpl.class);
  private static final String CACHE_PREFIX = "withdraw:";

  private final WithdrawCommandRepository repo;
  private final WithdrawQueryRepository queryRepo;
  private final CardClientRepository repoCard;
  private final SaldoClientRepository repoSaldo;
  private final RedisService redisService;
  private final KafkaService kafkaService;
  private final TracingMetrics tracingMetrics;

  private static final int DEFAULT_DAILY_WITHDRAW_LIMIT = 10_000_000;

  @Override
  public Future<WithdrawResponse> createWithdraw(CreateWithdrawRequest req) {
    var tracingCtx = tracingMetrics.startSpan(
        "WithdrawCommandService.createWithdraw",
        Attributes.builder()
            .put("card.number", req.getCardNumber())
            .put("withdraw.amount", (long) req.getWithdrawAmount())
            .build());
    Span span = Span.fromContext(Objects.requireNonNull(tracingCtx.getContext()));

    if (req.getWithdrawAmount() <= 0 || req.getCardNumber() == null || req.getCardNumber().isBlank()) {
      return Future.<WithdrawResponse>failedFuture(
          new BadRequestException("card_number and withdraw amount must be valid"));
    }

    String idempotencyKey = req.getIdempotencyKey();
    Future<Withdraw> existingFuture = idempotencyKey != null && !idempotencyKey.isBlank()
        ? repo.findByIdempotencyKey(idempotencyKey)
        : Future.succeededFuture();

    return existingFuture
        // Card validation also happens on replay, so a key cannot bypass the
        // normal card/user boundary.
        .compose(existing -> repoCard.findUserCardByCardNumber(req.getCardNumber())
            .map(card -> new WithdrawCreateContext(card, existing)))
        .compose(ctx -> {
          if (ctx.existing != null) {
            if (!matchesIdempotentRequest(ctx.existing, req)) {
              return Future.<WithdrawCreateContext>failedFuture(new ConflictException(
                  "Idempotency key was already used for a different withdrawal"));
            }
            span.addEvent("idempotent_replay");
            return Future.succeededFuture(ctx);
          }

          long dailyLimit = configuredDailyLimit();
          return queryRepo.getTodaySuccessfulAmount(req.getCardNumber())
              .compose(today -> {
                if (today + req.getWithdrawAmount() > dailyLimit) {
                  return Future.<WithdrawCreateContext>failedFuture(new BadRequestException(
                      "Daily withdrawal limit exceeded"));
                }
                return repoSaldo.getSaldoByCardNumber(req.getCardNumber())
                    .compose(saldo -> {
                      int currentBalance = saldo.getData().getTotalBalance();
                      if (currentBalance < req.getWithdrawAmount()) {
                        return Future.<WithdrawCreateContext>failedFuture(
                            new InsufficientBalanceException(currentBalance, req.getWithdrawAmount()));
                      }
                      return repo.createWithdraw(req, dailyLimit).compose(created -> {
                  if (created != null) {
                    ctx.withdraw = created;
                    return Future.succeededFuture(ctx);
                  }
                  if (idempotencyKey == null || idempotencyKey.isBlank()) {
                    return Future.<WithdrawCreateContext>failedFuture(
                        new IllegalStateException("Withdrawal insert returned no row"));
                  }
                  return repo.findByIdempotencyKey(idempotencyKey).compose(raced -> {
                    if (raced == null || !matchesIdempotentRequest(raced, req)) {
                      return Future.<WithdrawCreateContext>failedFuture(new ConflictException(
                          "Idempotency key was already used for a different withdrawal"));
                    }
                    ctx.existing = raced;
                    return Future.succeededFuture(ctx);
                      });
                    });
                });
              });
        })
        .compose(ctx -> {
          if (ctx.existing != null) {
            return Future.succeededFuture(ctx);
          }
          span.addEvent("saldo_debit_start");
          return repoSaldo.updateSaldoDelta(req.getCardNumber(), -req.getWithdrawAmount())
              .map(v -> ctx)
              .recover(err -> markWithdrawFailedAndFail(ctx.withdraw, err));
        })
        .compose(ctx -> {
          if (ctx.existing != null) {
            return Future.succeededFuture(ctx);
          }
          return repo.updateWithdrawStatus(UpdateWithdrawStatus.builder()
                  .withdrawId(ctx.withdraw.getId()).status("success").build())
              .map(updated -> {
                if (updated == null) {
                  throw new IllegalStateException("Withdrawal status update returned no row");
                }
                ctx.withdraw = updated;
                return ctx;
              })
              .recover(err -> compensateWithdrawAndFail(ctx, err));
        })
        .compose(ctx -> invalidateCache(ctx.existing != null ? ctx.existing.getId() : ctx.withdraw.getId())
            .map(v -> ctx))
        .compose(ctx -> {
          if (ctx.existing != null) {
            return Future.succeededFuture(ctx);
          }
          return sendWithdrawEmail(ctx.card.getEmail(), req.getWithdrawAmount(), ctx.withdraw.getId(), "create")
              .map(v -> ctx);
        })
        .map(ctx -> WithdrawResponse.from(ctx.existing != null ? ctx.existing : ctx.withdraw))
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingCtx, "create", "Withdrawal created successfully"))
        .onFailure(err -> {
          logger.error("Failed to create withdrawal for card: {}", req.getCardNumber(), err);
          tracingMetrics.completeSpanError(tracingCtx, "create", err.getMessage());
        });
  }

  private long configuredDailyLimit() {
    String value = System.getenv("WITHDRAW_DAILY_LIMIT");
    if (value == null || value.isBlank()) {
      return DEFAULT_DAILY_WITHDRAW_LIMIT;
    }
    try {
      return Math.max(1, Long.parseLong(value));
    } catch (NumberFormatException e) {
      logger.warn("Invalid WITHDRAW_DAILY_LIMIT; using default", e);
      return DEFAULT_DAILY_WITHDRAW_LIMIT;
    }
  }

  private boolean matchesIdempotentRequest(Withdraw existing, CreateWithdrawRequest req) {
    return Objects.equals(existing.getCardNumber(), req.getCardNumber())
        && Objects.equals(existing.getWithdrawAmount(), (long) req.getWithdrawAmount());
  }

  private Future<WithdrawCreateContext> markWithdrawFailedAndFail(Withdraw withdraw, Throwable err) {
    return repo.updateWithdrawStatus(UpdateWithdrawStatus.builder()
            .withdrawId(withdraw.getId()).status("failed").build())
        .recover(statusErr -> {
          logger.error("Failed to mark withdrawal {} as failed", withdraw.getId(), statusErr);
          return Future.succeededFuture(withdraw);
        })
        .compose(ignored -> Future.<WithdrawCreateContext>failedFuture(err));
  }

  private Future<WithdrawCreateContext> compensateWithdrawAndFail(WithdrawCreateContext ctx, Throwable err) {
    return repoSaldo.updateSaldoDelta(ctx.withdraw.getCardNumber(), ctx.withdraw.getWithdrawAmount().intValue())
        .recover(compensationErr -> {
          logger.error("Failed to compensate withdrawal {}", ctx.withdraw.getId(), compensationErr);
          return Future.succeededFuture();
        })
        .compose(v -> repo.updateWithdrawStatus(UpdateWithdrawStatus.builder()
                .withdrawId(ctx.withdraw.getId()).status("failed").build())
            .recover(statusErr -> Future.succeededFuture(ctx.withdraw))
            .compose(ignored -> Future.<WithdrawCreateContext>failedFuture(err)));
  }

  private static class WithdrawCreateContext {
    Withdraw existing;
    Withdraw withdraw;
    final pb.card.Card.CardWithEmailResponse card;

    WithdrawCreateContext(pb.card.Card.CardWithEmailResponse card, Withdraw existing) {
      this.card = card;
      this.existing = existing;
    }
  }

  @Override
  public Future<WithdrawResponse> updateWithdraw(UpdateWithdrawRequest req) {
    var tracingCtx = tracingMetrics.startSpan(
        "WithdrawCommandService.updateWithdraw",
        Attributes.builder()
            .put("withdraw.id", (long) req.getWithdrawId())
            .put("withdraw.amount", (long) req.getWithdrawAmount())
            .build());

    logger.info("Updating withdrawal: {}, amount: {}", req.getWithdrawId(), req.getWithdrawAmount());

    return queryRepo.getWithdrawById(req.getWithdrawId())
        .<WithdrawResponse>compose(existing -> {
          if (existing == null) {
            return Future.<WithdrawResponse>failedFuture(
                new NotFoundException("Withdrawal not found with id: " + req.getWithdrawId()));
          }

          int saldoDelta = existing.getWithdrawAmount().intValue() - req.getWithdrawAmount();
          // Positive delta restores funds when the withdrawal is reduced; negative
          // delta debits only the difference when the withdrawal is increased.
          return repoSaldo.updateSaldoDelta(req.getCardNumber(), saldoDelta)
              .compose(debitResult -> repo.updateWithdraw(req, configuredDailyLimit())
                  .compose(updated -> {
                    if (updated == null) {
                      return Future.<Withdraw>failedFuture(new NotFoundException(
                          "Withdrawal not found with id: " + req.getWithdrawId()));
                    }
                    return repo.updateWithdrawStatus(UpdateWithdrawStatus.builder()
                            .withdrawId(updated.getId()).status("success").build())
                        .map(statusUpdated -> statusUpdated != null ? statusUpdated : updated);
                  })
                  .compose(updated -> invalidateCache(updated.getId()).<Withdraw>map(v -> updated))
                  .compose(updated -> repoCard.findUserCardByCardNumber(req.getCardNumber())
                      .compose(cardResp -> sendWithdrawEmail(
                          cardResp.getEmail(), req.getWithdrawAmount(), updated.getId(), "update")
                          .<Withdraw>map(v -> updated)))
                  .map(updated -> {
                    tracingMetrics.completeSpanSuccess(tracingCtx, "update",
                        "Withdrawal updated successfully");
                    return WithdrawResponse.from(updated);
                  })
                  .recover(err -> {
                    logger.error("Rolling back balance for withdraw update", err);
                    return repoSaldo.updateSaldoDelta(req.getCardNumber(), -saldoDelta)
                        .recover(compensationErr -> {
                          logger.error("Failed to compensate withdraw update {}", req.getWithdrawId(), compensationErr);
                          return Future.succeededFuture();
                        })
                        .compose(ignored -> Future.<WithdrawResponse>failedFuture(err));
                  }));
        })
        .onFailure(err -> {
          logger.error("Failed to update withdrawal: {}", req.getWithdrawId(), err);
          tracingMetrics.completeSpanError(tracingCtx, "update", err.getMessage());
        });
  }

  @Override
  public Future<WithdrawResponseDeleteAt> trashWithdraw(Integer withdrawId) {
    var tracingCtx = tracingMetrics.startSpan(
        "WithdrawCommandService.trashWithdraw",
        Attributes.builder().put("withdraw.id", (long) withdrawId).build());

    logger.info("Trashing withdrawal: {}", withdrawId);

    return repo.trashWithdraw(withdrawId)
        .compose(withdraw -> {
          if (withdraw == null) {
            return Future.failedFuture(
                new NotFoundException("Withdrawal not found with id: " + withdrawId));
          }
          return invalidateCache(withdrawId).map(v -> withdraw);
        })
        .map(withdraw -> {
          tracingMetrics.completeSpanSuccess(tracingCtx, "trashed",
              "Withdrawal trashed successfully");
          return WithdrawResponseDeleteAt.from(withdraw);
        })
        .onFailure(err -> {
          logger.error("Failed to trash withdrawal: {}", withdrawId, err);
          tracingMetrics.completeSpanError(tracingCtx, "trashed", err.getMessage());
        });
  }

  @Override
  public Future<WithdrawResponseDeleteAt> restoreWithdraw(Integer withdrawId) {
    var tracingCtx = tracingMetrics.startSpan(
        "WithdrawCommandService.restoreWithdraw",
        Attributes.builder().put("withdraw.id", (long) withdrawId).build());

    logger.info("Restoring withdrawal: {}", withdrawId);

    return queryRepo.findByTrashed(withdrawId)
        .compose(trashed -> {
          if (trashed == null)
            return Future.failedFuture(new BadRequestException("Withdrawal not found or must be trashed first"));
          return repo.restoreWithdraw(withdrawId);
        })
        .compose(withdraw -> {
          if (withdraw == null) {
            return Future.<Withdraw>failedFuture(new NotFoundException("Withdrawal not found with id: " + withdrawId));
          }
          return invalidateCache(withdrawId).<Withdraw>map(v -> withdraw);
        })
        .map(WithdrawResponseDeleteAt::from)
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingCtx, "restore", "Withdrawal restored successfully"))
        .onFailure(err -> {
          logger.error("Failed to restore withdrawal: {}", withdrawId, err);
          tracingMetrics.completeSpanError(tracingCtx, "restore", err.getMessage());
        });
  }

  @Override
  public Future<Void> deleteWithdrawPermanently(Integer withdrawId) {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan(
        "WithdrawCommandService.deleteWithdrawPermanently",
        Attributes.builder().put("withdraw.id", withdrawId).build());

    return queryRepo.findByTrashed(withdrawId)
        .compose(trashed -> {
          if (trashed == null) {
            return Future.<Void>failedFuture(
                new BadRequestException("Withdrawal not found or must be trashed first"));
          }
          return repo.deleteWithdrawPermanently(withdrawId)
              .compose(deleted -> {
                if (!deleted) {
                  return Future.<Void>failedFuture(
                      new BadRequestException("Withdrawal not found or must be trashed first"));
                }
                return invalidateCache(withdrawId);
              });
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx,
            "deletePermanent", "Withdraw deleted permanently"))
        .onFailure(err -> {
          logger.error("Failed to deletePermanent withdrawal: {}", withdrawId, err);
          tracingMetrics.completeSpanError(ctx, "deletePermanent", err.getMessage());
        });
  }

  public Future<Void> restoreAllWithdraws() {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan(
        "WithdrawService.restoreAll");

    return repo.restoreAllWithdraws()
        .compose(count -> {
          if (count == 0) {
            return Future.<Void>failedFuture(
                new NotFoundException("No trashed withdrawals found"));
          }
          return redisService.delete("withdraw:list:*").mapEmpty();
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx,
            "restore_all", "All withdrawals restored"))
        .onFailure(err -> {
          logger.error("Failed to restore all withdrawals", err);
          tracingMetrics.completeSpanError(ctx, "restore_all", err.getMessage());
        });
  }

  public Future<Void> deleteAllPermanentWithdraws() {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan(
        "WithdrawService.deleteAllPermanent");

    return repo.deleteAllPermanentWithdraws()
        .compose(count -> {
          if (count == 0) {
            return Future.<Void>failedFuture(
                new NotFoundException("No trashed withdrawals found"));
          }
          return redisService.delete("withdraw:list:*").mapEmpty();
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx,
            "deleteAllPermanent", "All withdrawals permanently deleted"))
        .onFailure(err -> {
          logger.error("Failed to permanently delete all withdrawals", err);
          tracingMetrics.completeSpanError(ctx, "deleteAllPermanent", err.getMessage());
        });
  }

  private Future<Void> invalidateCache(Integer withdrawId) {
    return redisService.delete(CACHE_PREFIX + withdrawId)
        .compose(v -> redisService.delete(CACHE_PREFIX + "list:*"))
        .mapEmpty();
  }

  private Future<Void> sendWithdrawEmail(String email, int amount, Integer withdrawId, String operation) {
    String title = "create".equals(operation) ? "Withdraw Successful" : "Withdraw Updated";
    String subject = "create".equals(operation)
        ? "Withdraw Successful - SanEdge"
        : "Withdraw Updated - SanEdge";
    String topic = "create".equals(operation)
        ? "email-service-topic-withdraw-create"
        : "email-service-topic-withdraw-update";

    String htmlBody = EmailTemplate.generateHtml(Map.of(
        "Title", title,
        "Message", String.format("Your withdrawal of %d has been processed successfully.", amount),
        "Button", "View History",
        "Link", "https://sanedge.example.com/withdraw/history"));

    JsonObject emailPayload = new JsonObject()
        .put("email", email)
        .put("subject", subject)
        .put("body", htmlBody);

    return kafkaService.sendMessage(topic, String.valueOf(withdrawId), emailPayload)
        .<Void>mapEmpty()
        .onFailure(err -> logger.warn(
            "Failed to send withdraw {} email via Kafka for withdrawId: {}",
            operation, withdrawId, err))
        .recover(err -> Future.succeededFuture());
  }
}