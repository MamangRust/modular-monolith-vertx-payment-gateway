package io.example.withdraw.service.impl;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.InsufficientBalanceException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.example.common.utils.EmailTemplate;
import io.example.withdraw.domain.requests.CreateWithdrawRequest;
import io.example.withdraw.domain.requests.UpdateSaldoBalance;
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

  @Override
  public Future<WithdrawResponse> createWithdraw(CreateWithdrawRequest req) {
    var tracingCtx = tracingMetrics.startSpan(
        "WithdrawCommandService.createWithdraw",
        Attributes.builder()
            .put("card.number", req.getCardNumber())
            .put("withdraw.amount", (long) req.getWithdrawAmount())
            .build());

    Span span = Span.fromContext(Objects.requireNonNull(tracingCtx.getContext()));
    logger.info("Creating withdrawal for card: {}, amount: {}", req.getCardNumber(), req.getWithdrawAmount());

    return repoCard.findUserCardByCardNumber(req.getCardNumber())
        .<WithdrawResponse>compose(cardResp -> repoSaldo.getSaldoByCardNumber(req.getCardNumber())
            .<WithdrawResponse>compose(saldoResp -> {
              int currentBalance = saldoResp.getData().getTotalBalance();

              if (currentBalance < req.getWithdrawAmount()) {
                return Future.<WithdrawResponse>failedFuture(
                    new InsufficientBalanceException(currentBalance, req.getWithdrawAmount()));
              }

              int newBalance = currentBalance - req.getWithdrawAmount();
              span.addEvent("deducting_saldo");

              // ✅ FIX 1: Gunakan domain request builder untuk update saldo
              var saldoReq = UpdateSaldoBalance.builder()
                  .cardNumber(req.getCardNumber())
                  .totalBalance(newBalance)
                  .build();

              return repoSaldo.updateSaldoBalance(saldoReq)
                  .compose(v -> {
                    span.addEvent("creating_withdraw_record");
                    return repo.createWithdraw(req);
                  })
                  .compose(withdraw -> {
                    span.addEvent("marking_withdraw_success");
                    // ✅ FIX 2: Gunakan domain request builder untuk update status
                    var statusReq = UpdateWithdrawStatus.builder()
                        .withdrawId(withdraw.getId())
                        .status("success")
                        .build();
                    return repo.updateWithdrawStatus(statusReq);
                  })
                  .compose(updated -> invalidateCache(updated.getId())
                      .<Withdraw>map(v -> updated)) // ✅ FIX 3: Type witness <Withdraw>
                  .compose(updated -> sendWithdrawEmail(
                      cardResp.getEmail(), req.getWithdrawAmount(), updated.getId(), "create")
                      .<Withdraw>map(v -> updated)) // ✅ FIX 3: Type witness <Withdraw>
                  .map(updated -> {
                    tracingMetrics.completeSpanSuccess(tracingCtx, "create",
                        "Withdrawal created successfully");
                    return WithdrawResponse.from(updated);
                  })
                  .recover(err -> {
                    logger.error("Failed to create withdraw record, rolling back balance", err);
                    // ✅ FIX 4: Gunakan domain request builder untuk rollback saldo
                    var rollbackReq = UpdateSaldoBalance.builder()
                        .cardNumber(req.getCardNumber())
                        .totalBalance(currentBalance)
                        .build();
                    return repoSaldo.updateSaldoBalance(rollbackReq)
                        .compose(rv -> Future.<WithdrawResponse>failedFuture(err));
                  });
            }))
        .onFailure(err -> {
          logger.error("Failed to create withdrawal for card: {}", req.getCardNumber(), err);
          tracingMetrics.completeSpanError(tracingCtx, "create", err.getMessage());
        });
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

          int diff = req.getWithdrawAmount() - existing.getWithdrawAmount().intValue();

          return repoSaldo.getSaldoByCardNumber(req.getCardNumber())
              .<WithdrawResponse>compose(saldoResp -> {
                int currentBalance = saldoResp.getData().getTotalBalance();
                int newBalance = currentBalance - diff;

                if (newBalance < 0) {
                  // ✅ FIX 5: Gunakan domain request builder untuk status failed
                  var failedStatusReq = UpdateWithdrawStatus.builder()
                      .withdrawId(req.getWithdrawId())
                      .status("failed")
                      .build();
                  return repo.updateWithdrawStatus(failedStatusReq)
                      .compose(v -> Future.<WithdrawResponse>failedFuture(
                          new InsufficientBalanceException(currentBalance,
                              req.getWithdrawAmount())));
                }

                // ✅ FIX 6: Gunakan domain request builder untuk update saldo
                var saldoReq = UpdateSaldoBalance.builder()
                    .cardNumber(req.getCardNumber())
                    .totalBalance(newBalance)
                    .build();

                return repoSaldo.updateSaldoBalance(saldoReq)
                    .compose(v -> repo.updateWithdraw(req)) // Sesuai snippet repo yang terima UpdateWithdrawRequest
                    .compose(updated -> {
                      // ✅ FIX 7: Gunakan domain request builder untuk status success
                      var statusReq = UpdateWithdrawStatus.builder()
                          .withdrawId(updated.getId())
                          .status("success")
                          .build();
                      return repo.updateWithdrawStatus(statusReq);
                    })
                    .compose(updated -> invalidateCache(updated.getId())
                        .<Withdraw>map(v -> updated)) // ✅ FIX 8: Type witness <Withdraw>
                    .compose(updated -> repoCard.findUserCardByCardNumber(req.getCardNumber())
                        .compose(cardResp -> sendWithdrawEmail(
                            cardResp.getEmail(), req.getWithdrawAmount(),
                            updated.getId(), "update")
                            .<Withdraw>map(v -> updated))) // ✅ FIX 8: Type witness <Withdraw>
                    .map(updated -> {
                      tracingMetrics.completeSpanSuccess(tracingCtx, "update",
                          "Withdrawal updated successfully");
                      return WithdrawResponse.from(updated);
                    })
                    .recover(err -> {
                      logger.error("Rolling back balance for withdraw update", err);
                      var rollbackReq = UpdateSaldoBalance.builder()
                          .cardNumber(req.getCardNumber())
                          .totalBalance(currentBalance)
                          .build();
                      return repoSaldo.updateSaldoBalance(rollbackReq)
                          .compose(rv -> Future.<WithdrawResponse>failedFuture(err));
                    });
              });
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