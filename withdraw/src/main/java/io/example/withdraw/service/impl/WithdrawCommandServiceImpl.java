package io.example.withdraw.service.impl;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.exception.BadRequestException;
import io.example.common.exception.NotFoundException;
import io.example.common.model.ApiResponse;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.example.common.utils.EmailTemplate;
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
import pb.withdraw.WithdrawCommand.CreateWithdrawRequest;
import pb.withdraw.WithdrawCommand.UpdateWithdrawRequest;

public class WithdrawCommandServiceImpl implements WithdrawCommandService {
  private static final Logger logger = LoggerFactory.getLogger(WithdrawCommandServiceImpl.class);

  private final WithdrawCommandRepository repo;
  private final WithdrawQueryRepository queryRepo;
  private final CardClientRepository repoCard;
  private final SaldoClientRepository repoSaldo;
  private final RedisService redisService;
  private final KafkaService kafkaService;
  private final TracingMetrics tracingMetrics;

  private static final String CACHE_PREFIX = "withdraw:";

  public WithdrawCommandServiceImpl(
      WithdrawCommandRepository repo,
      WithdrawQueryRepository queryRepo,
      CardClientRepository repoCard,
      SaldoClientRepository repoSaldo,
      RedisService redisService,
      KafkaService kafkaService,
      TracingMetrics tracingMetrics) {
    this.repo = repo;
    this.queryRepo = queryRepo;
    this.repoCard = repoCard;
    this.repoSaldo = repoSaldo;
    this.redisService = redisService;
    this.kafkaService = kafkaService;
    this.tracingMetrics = tracingMetrics;
  }

  @Override
  public Future<ApiResponse<WithdrawResponse>> createWithdraw(CreateWithdrawRequest req) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "WithdrawCommandService.createWithdraw",
        Attributes.builder()
            .put("card.number", Objects.requireNonNull(req.getCardNumber()))
            .put("withdraw.amount", (long) req.getWithdrawAmount())
            .build());

    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    logger.info("Creating withdrawal for card: {}, amount: {}", req.getCardNumber(), req.getWithdrawAmount());

    return repoCard.findUserCardByCardNumber(req.getCardNumber())
        .compose(cardResp -> repoSaldo.getSaldoByCardNumber(req.getCardNumber())
            .compose(saldoResp -> {
              int currentBalance = saldoResp.getData().getTotalBalance();
              if (currentBalance < req.getWithdrawAmount()) {
                return Future.failedFuture(new BadRequestException("Insufficient balance"));
              }

              int newBalance = currentBalance - req.getWithdrawAmount();
              span.addEvent("deducting_saldo");

              return repoSaldo.updateSaldoBalance(req.getCardNumber(), newBalance)
                  .compose(v -> {
                    span.addEvent("creating_withdraw_record");
                    return repo.createWithdraw(req.getCardNumber(), req.getWithdrawAmount())
                        .compose(withdraw -> {
                          span.addEvent("marking_withdraw_success");
                          return repo.updateWithdrawStatus(withdraw.getId(), "success")
                              .compose(updated -> redisService.delete(CACHE_PREFIX + "list:*")
                                  .map(ignored -> updated))
                              .compose(updated -> {
                                // Send Email Notification (Async)
                                String email = cardResp.getEmail();
                                String htmlBody = EmailTemplate.generateHtml(Map.of(
                                    "Title", "Withdraw Successful",
                                    "Message",
                                    String.format("Your withdrawal of %d has been processed successfully.",
                                        req.getWithdrawAmount()),
                                    "Button", "View History",
                                    "Link", "https://sanedge.example.com/withdraw/history"));

                                JsonObject emailPayload = new JsonObject()
                                    .put("email", email)
                                    .put("subject", "Withdraw Successful - SanEdge")
                                    .put("body", htmlBody);

                                return kafkaService.sendMessage("email-service-topic-withdraw-create",
                                    String.valueOf(updated.getId()), emailPayload)
                                    .map(v2 -> {
                                      tracingMetrics.completeSpanSuccess(tracingContext, "create",
                                          "Withdrawal created successfully");
                                      return ApiResponse.success("Withdrawal created successfully",
                                          WithdrawResponse.from(updated));
                                    })
                                    .recover(err -> {
                                      logger.error("Failed to send withdraw email via Kafka for withdrawId: {}", updated.getId(), err);
                                      tracingMetrics.completeSpanSuccess(tracingContext, "create",
                                          "Withdrawal created successfully (email failed)");
                                      return Future.succeededFuture(ApiResponse.success("Withdrawal created successfully",
                                          WithdrawResponse.from(updated)));
                                    });
                              });
                        });
                  })
                  .recover(err -> {
                    logger.error("Failed to create withdraw record, rolling back balance", err);
                    return repoSaldo.updateSaldoBalance(req.getCardNumber(), currentBalance)
                        .compose(rv -> Future.failedFuture(err));
                  });
            }))
        .recover(err -> {
          logger.error("Failed to create withdrawal", err);
          tracingMetrics.completeSpanError(tracingContext, "create", err.getMessage());
          return Future.succeededFuture(ApiResponse.error(err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<WithdrawResponse>> updateWithdraw(UpdateWithdrawRequest req) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "WithdrawCommandService.updateWithdraw",
        Attributes.builder()
            .put("withdraw.id", (long) req.getWithdrawId())
            .put("withdraw.amount", (long) req.getWithdrawAmount())
            .build());

    logger.info("Updating withdrawal: {}, amount: {}", req.getWithdrawId(), req.getWithdrawAmount());

    return queryRepo.getWithdrawById(req.getWithdrawId())
        .compose(existing -> {
          if (existing == null)
            return Future.failedFuture(new NotFoundException("Withdrawal not found"));

          int diff = req.getWithdrawAmount() - existing.getWithdrawAmount().intValue();

          return repoSaldo.getSaldoByCardNumber(req.getCardNumber())
              .compose(saldoResp -> {
                int currentBalance = saldoResp.getData().getTotalBalance();
                int newBalance = currentBalance - diff;

                if (newBalance < 0) {
                  return repo.updateWithdrawStatus(req.getWithdrawId(), "failed")
                      .compose(v -> Future.failedFuture(new BadRequestException("Insufficient balance for update")));
                }

                return repoSaldo.updateSaldoBalance(req.getCardNumber(), newBalance)
                    .compose(v -> repo.updateWithdraw(req.getWithdrawId(), req.getCardNumber(), req.getWithdrawAmount())
                        .compose(updated -> repo.updateWithdrawStatus(updated.getId(), "success"))
                        .compose(updated -> redisService.delete(CACHE_PREFIX + req.getWithdrawId()).map(ignored -> updated))
                        .compose(updated -> redisService.delete(CACHE_PREFIX + "list:*").map(ignored -> updated))
                        .compose(updated -> repoCard.findUserCardByCardNumber(req.getCardNumber())
                            .compose(cardResp -> {
                              String email = cardResp.getEmail();
                              String htmlBody = EmailTemplate.generateHtml(Map.of(
                                  "Title", "Withdraw Updated",
                                  "Message",
                                  String.format("Your withdrawal has been updated to %d successfully.",
                                      req.getWithdrawAmount()),
                                  "Button", "View History",
                                  "Link", "https://sanedge.example.com/withdraw/history"));

                              JsonObject emailPayload = new JsonObject()
                                  .put("email", email)
                                  .put("subject", "Withdraw Updated - SanEdge")
                                  .put("body", htmlBody);

                              return kafkaService.sendMessage("email-service-topic-withdraw-update",
                                  String.valueOf(updated.getId()), emailPayload);
                            })
                            .map(v2 -> {
                              tracingMetrics.completeSpanSuccess(tracingContext, "update", "Withdrawal updated successfully");
                              return ApiResponse.success("Withdrawal updated successfully", WithdrawResponse.from(updated));
                            })
                            .recover(err -> {
                              logger.error("Failed to send withdraw update email via Kafka for withdrawId: {}", updated.getId(), err);
                              tracingMetrics.completeSpanSuccess(tracingContext, "update", "Withdrawal updated successfully (email failed)");
                              return Future.succeededFuture(ApiResponse.success("Withdrawal updated successfully", WithdrawResponse.from(updated)));
                            })))
                    .recover(err -> repoSaldo.updateSaldoBalance(req.getCardNumber(), currentBalance)
                        .compose(rv -> Future.failedFuture(err)));
              });
        })
        .recover(err -> {
          logger.error("Failed to update withdrawal: {}", req.getWithdrawId(), err);
          tracingMetrics.completeSpanError(tracingContext, "update", err.getMessage());
          return Future.succeededFuture(ApiResponse.error(err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<WithdrawResponseDeleteAt>> trashWithdraw(Integer withdrawId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "WithdrawCommandService.trashWithdraw",
        Attributes.builder()
            .put("withdraw.id", (long) withdrawId)
            .build());

    logger.info("Trashing withdrawal: {}", withdrawId);

    return repo.trashWithdraw(withdrawId)
        .compose(withdraw -> {
          if (withdraw == null) {
            return Future.failedFuture(new NotFoundException("Withdrawal not found with id: " + withdrawId));
          }
          return redisService.delete(CACHE_PREFIX + withdrawId)
              .compose(v -> redisService.delete(CACHE_PREFIX + "list:*"))
              .map(v -> withdraw);
        })
        .map(withdraw -> {
          tracingMetrics.completeSpanSuccess(tracingContext, "trashed", "Withdrawal trashed successfully");
          return ApiResponse.success("Withdrawal trashed successfully", WithdrawResponseDeleteAt.from(withdraw));
        })
        .recover(err -> {
          logger.error("Failed to trash withdrawal: {}", withdrawId, err);
          tracingMetrics.completeSpanError(tracingContext, "trashed", err.getMessage());
          return Future.succeededFuture(ApiResponse.error(err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<WithdrawResponseDeleteAt>> restoreWithdraw(Integer withdrawId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "WithdrawCommandService.restoreWithdraw",
        Attributes.builder()
            .put("withdraw.id", (long) withdrawId)
            .build());

    logger.info("Restoring withdrawal: {}", withdrawId);

    return repo.restoreWithdraw(withdrawId)
        .compose(withdraw -> {
          if (withdraw == null) {
            return Future.failedFuture(new NotFoundException("Withdrawal not found with id: " + withdrawId));
          }
          return redisService.delete(CACHE_PREFIX + withdrawId)
              .compose(v -> redisService.delete(CACHE_PREFIX + "list:*"))
              .map(v -> withdraw);
        })
        .map(withdraw -> {
          tracingMetrics.completeSpanSuccess(tracingContext, "restore", "Withdrawal restored successfully");
          return ApiResponse.success("Withdrawal restored successfully", WithdrawResponseDeleteAt.from(withdraw));
        })
        .recover(err -> {
          logger.error("Failed to restore withdrawal: {}", withdrawId, err);
          tracingMetrics.completeSpanError(tracingContext, "restore", err.getMessage());
          return Future.succeededFuture(ApiResponse.error(err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<Void>> deleteWithdrawPermanently(Integer withdrawId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "WithdrawCommandService.deleteWithdrawPermanently",
        Attributes.builder()
            .put("withdraw.id", (long) withdrawId)
            .build());

    logger.info("Permanently deleting withdrawal: {}", withdrawId);

    return repo.deleteWithdrawPermanently(withdrawId)
        .compose(v -> redisService.delete(CACHE_PREFIX + withdrawId))
        .compose(v -> redisService.delete(CACHE_PREFIX + "list:*"))
        .map(v -> {
          tracingMetrics.completeSpanSuccess(tracingContext, "deletePermanent", "Withdrawal deleted permanently");
          return ApiResponse.<Void>success("success", null);
        })
        .recover(throwable -> {
          logger.error("Failed to deletePermanent withdrawal: {}", withdrawId, throwable);
          tracingMetrics.completeSpanError(tracingContext, "deletePermanent", throwable.getMessage());
          return Future.succeededFuture(ApiResponse.error(throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<Void>> restoreAllWithdraws() {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("WithdrawCommandService.restoreAll");

    logger.info("Attempting to restore all trashed withdrawals");

    return repo.restoreAllWithdraws()
        .compose(v -> redisService.delete(CACHE_PREFIX + "list:*"))
        .map(v -> {
          tracingMetrics.completeSpanSuccess(tracingContext, "restore_all", "All withdrawals restored");
          return ApiResponse.<Void>success("All withdrawals restored successfully");
        })
        .recover(throwable -> {
          logger.error("Failed to restore all withdrawals", throwable);
          tracingMetrics.completeSpanError(tracingContext, "restore_all", throwable.getMessage());
          return Future.succeededFuture(ApiResponse.error(throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<Void>> deleteAllPermanentWithdraws() {
    TracingMetrics.TracingContext tracingContext = tracingMetrics
        .startSpan("WithdrawCommandService.deleteAllPermanent");

    logger.info("Attempting to permanently delete all trashed withdrawals");

    return repo.deleteAllPermanentWithdraws()
        .compose(v -> redisService.delete(CACHE_PREFIX + "list:*"))
        .map(v -> {
          tracingMetrics.completeSpanSuccess(tracingContext, "deleteAllPermanent",
              "All withdrawals permanently deleted");
          return ApiResponse.<Void>success("All withdrawals permanently deleted");
        })
        .recover(throwable -> {
          logger.error("Failed to delete all permanent withdrawals", throwable);
          tracingMetrics.completeSpanError(tracingContext, "deleteAllPermanent", throwable.getMessage());
          return Future.succeededFuture(ApiResponse.error(throwable.getMessage()));
        });
  }
}
