package io.example.topup.service.impl;

import java.time.Instant;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.exception.NotFoundException;
import io.example.common.model.ApiResponse;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.common.service.KafkaService;
import io.example.common.utils.EmailTemplate;
import io.example.topup.domain.requests.topup.CreateTopupRequest;
import io.example.topup.domain.requests.topup.UpdateTopupAmount;
import io.example.topup.domain.requests.topup.UpdateTopupRequest;
import io.example.topup.domain.requests.topup.UpdateTopupStatus;
import io.example.topup.model.Topup;
import io.example.topup.model.TopupResponse;
import io.example.topup.model.TopupResponseDeleteAt;
import io.example.topup.repository.CardClientRepository;
import io.example.topup.repository.SaldoClientRepository;
import io.example.topup.repository.TopupCommandRepository;
import io.example.topup.repository.TopupQueryRepository;
import io.example.topup.service.TopupCommandService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import pb.card.Card.CardWithEmailResponse;
import pb.card.CardCommand.UpdateCardRequest;

public class TopupCommandServiceImpl implements TopupCommandService {
  private static final Logger logger = LoggerFactory.getLogger(TopupCommandServiceImpl.class);

  private final TopupCommandRepository repo;
  private final TopupQueryRepository repoQuery;
  private final CardClientRepository repoCard;
  private final SaldoClientRepository repoSaldo;
  private final RedisService redisService;
  private final TracingMetrics tracingMetrics;
  private final KafkaService kafkaService;

  private static final String CACHE_PREFIX = "topup:";

  public TopupCommandServiceImpl(
      TopupCommandRepository repo,
      TopupQueryRepository repoQuery,
      CardClientRepository repoCard,
      SaldoClientRepository repoSaldo,
      RedisService redisService,
      TracingMetrics tracingMetrics,
      KafkaService kafkaService) {
    this.repo = repo;
    this.repoQuery = repoQuery;
    this.repoCard = repoCard;
    this.repoSaldo = repoSaldo;
    this.redisService = redisService;
    this.tracingMetrics = tracingMetrics;
    this.kafkaService = kafkaService;
  }

  @Override
  public Future<ApiResponse<TopupResponse>> createTopup(pb.topup.TopupCommand.CreateTopupRequest req) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TopupCommandService.createTopup",
        Attributes.builder()
            .put("topup.card_number", Objects.requireNonNull(req.getCardNumber()))
            .put("topup.amount", (long) req.getTopupAmount())
            .build());

    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    logger.info("Creating topup for card: {} with amount: {}", req.getCardNumber(), req.getTopupAmount());

    return repoCard.getCardEmailByCardNumber(req.getCardNumber())
        .compose((CardWithEmailResponse card) -> {
          span.addEvent("card_found");
          span.setAttribute("card.id", (long) card.getId());

          CreateTopupRequest domainReq = CreateTopupRequest.builder()
              .cardNumber(req.getCardNumber())
              .topupNo(req.getTopupNo())
              .topupAmount(req.getTopupAmount())
              .topupMethod(req.getTopupMethod())
              .build();

          return repo.createTopup(domainReq)
              .map((Topup topup) -> {
                span.addEvent("topup_created");
                span.setAttribute("topup.id", (long) topup.getId());
                return new CardTopupContext(card, topup);
              });
        })
        .compose((CardTopupContext ctx) -> repoSaldo.getSaldoByCardNumber(req.getCardNumber())
            .compose(saldoResp -> {
              int currentBalance = saldoResp.getData().getTotalBalance();
              int newBalance = currentBalance + req.getTopupAmount();

              span.addEvent("saldo_update_start");
              span.setAttribute("saldo.old_balance", (long) currentBalance);
              span.setAttribute("saldo.new_balance", (long) newBalance);

              return repoSaldo.updateSaldoBalance(req.getCardNumber(), newBalance)
                  .map(v -> {
                    span.addEvent("saldo_updated");
                    return ctx;
                  });
            })
            .recover(err -> {
              span.recordException(Objects.requireNonNull(err));
              span.addEvent("saldo_update_failed");
              UpdateTopupStatus statusReq = UpdateTopupStatus.builder()
                  .topupId(ctx.topup.getId())
                  .status("failed")
                  .build();
              return repo.updateTopupStatus(statusReq)
                  .compose(v -> Future.failedFuture(err));
            }))
        .compose((CardTopupContext ctx) -> {
          try {
            CardWithEmailResponse card = ctx.card;
            span.addEvent("card_update_start");

            Instant expireInstant = Instant.parse(card.getExpireDate());
            com.google.protobuf.Timestamp expireTs = com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(expireInstant.getEpochSecond())
                .setNanos(expireInstant.getNano())
                .build();

            UpdateCardRequest updateCardRequest = UpdateCardRequest.newBuilder()
                .setCardId(card.getId())
                .setUserId(card.getUserId())
                .setCardType(card.getCardType())
                .setExpireDate(expireTs)
                .setCvv(card.getCvv())
                .setCardProvider(card.getCardProvider())
                .build();

            return repoCard.updateCard(updateCardRequest)
                .map(v -> {
                  span.addEvent("card_updated");
                  return ctx;
                })
                .recover(err -> {
                  span.recordException(Objects.requireNonNull(err));
                  span.addEvent("card_update_failed");
                  UpdateTopupStatus statusReq = UpdateTopupStatus.builder()
                      .topupId(ctx.topup.getId())
                      .status("failed")
                      .build();
                  return repo.updateTopupStatus(statusReq)
                      .compose(v -> Future.failedFuture(err));
                });
          } catch (Exception e) {
            span.recordException(e);
            span.addEvent("card_update_exception");
            UpdateTopupStatus statusReq = UpdateTopupStatus.builder()
                .topupId(ctx.topup.getId())
                .status("failed")
                .build();
            return repo.updateTopupStatus(statusReq)
                .compose(v -> Future.failedFuture(e));
          }
        })
        .compose((CardTopupContext ctx) -> {
          span.addEvent("topup_mark_success");
          UpdateTopupStatus statusReq = UpdateTopupStatus.builder()
              .topupId(ctx.topup.getId())
              .status("success")
              .build();
          return repo.updateTopupStatus(statusReq)
              .map(v -> ctx);
        })
        .compose((CardTopupContext ctx) -> {
          // Send Email via Kafka (Async but in chain)
          if (kafkaService != null && ctx.card.getEmail() != null && !ctx.card.getEmail().isEmpty()) {
            String htmlBody = EmailTemplate.generateHtml(java.util.Map.of(
                "Title", "Topup Successful",
                "Message", String.format("Your topup of %d has been processed successfully.", req.getTopupAmount()),
                "Button", "View History",
                "Link", "https://sanedge.example.com/topup/history"));

            JsonObject emailPayload = new JsonObject()
                .put("email", ctx.card.getEmail())
                .put("subject", "Topup Successful - SanEdge")
                .put("body", htmlBody);

            return kafkaService.sendMessage("email-service-topic-topup-create", String.valueOf(ctx.topup.getId()), emailPayload)
                .map(v -> {
                  tracingMetrics.completeSpanSuccess(tracingContext, "create", "Topup created successfully");
                  logger.info("Topup created successfully. topupId={}, card={}", ctx.topup.getId(), req.getCardNumber());
                  return ApiResponse.success("Topup created successfully", TopupResponse.from(ctx.topup));
                })
                .recover(err -> {
                  logger.error("Failed to send topup email via Kafka for topupId: {}", ctx.topup.getId(), err);
                  tracingMetrics.completeSpanSuccess(tracingContext, "create", "Topup created successfully (email failed)");
                  return Future.succeededFuture(ApiResponse.success("Topup created successfully", TopupResponse.from(ctx.topup)));
                });
          }

          tracingMetrics.completeSpanSuccess(tracingContext, "create", "Topup created successfully");
          logger.info("Topup created successfully. topupId={}, card={}", ctx.topup.getId(), req.getCardNumber());
          return Future.succeededFuture(ApiResponse.success("Topup created successfully", TopupResponse.from(ctx.topup)));
        })
        .recover(err -> {
          logger.error("Failed to create topup for card {}", req.getCardNumber(), err);
          span.recordException(Objects.requireNonNull(err));
          tracingMetrics.completeSpanError(tracingContext, "create", err.getMessage());
          return Future.succeededFuture(ApiResponse.error("Failed to create topup: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<TopupResponse>> updateTopup(pb.topup.TopupCommand.UpdateTopupRequest req) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TopupCommandService.updateTopup",
        Attributes.builder()
            .put("topup.card_number", Objects.requireNonNull(req.getCardNumber()))
            .put("topup.topup_id", (long) req.getTopupId())
            .put("topup.amount", (long) req.getTopupAmount())
            .build());

    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    logger.info("Updating topup for card: {}, topupId: {}, amount: {}", req.getCardNumber(), req.getTopupId(),
        req.getTopupAmount());

    return repoCard.getCardByCardNumber(req.getCardNumber())
        .compose(cardResp -> {
          span.addEvent("card_found");
          span.setAttribute("card.id", (long) cardResp.getData().getId());

          return repoQuery.getTopupById(req.getTopupId())
              .map((Topup existingTopup) -> {
                span.addEvent("existing_topup_found");
                span.setAttribute("topup.existing_amount", (long) existingTopup.getTopupAmount());
                return new UpdateTopupContext(existingTopup);
              });
        })
        .recover(err -> {
          span.recordException(Objects.requireNonNull(err));
          span.addEvent("card_or_topup_not_found");
          UpdateTopupStatus statusReq = UpdateTopupStatus.builder()
              .topupId(req.getTopupId())
              .status("failed")
              .build();
          return repo.updateTopupStatus(statusReq)
              .compose(v -> Future.failedFuture(err));
        })
        .compose((UpdateTopupContext ctx) -> {
          int topupDifference = req.getTopupAmount() - ctx.existingTopup.getTopupAmount().intValue();
          span.addEvent("topup_update_start");
          span.setAttribute("topup.difference", (long) topupDifference);

          UpdateTopupRequest domainReq = UpdateTopupRequest.builder()
              .topupId(req.getTopupId())
              .cardNumber(req.getCardNumber())
              .topupAmount(req.getTopupAmount())
              .topupMethod(req.getTopupMethod())
              .build();

          return repo.updateTopup(domainReq)
              .map(v -> {
                span.addEvent("topup_updated");
                ctx.topupDifference = topupDifference;
                return ctx;
              })
              .recover(err -> {
                span.recordException(Objects.requireNonNull(err));
                span.addEvent("topup_update_failed");
                UpdateTopupStatus statusReq = UpdateTopupStatus.builder()
                    .topupId(req.getTopupId())
                    .status("failed")
                    .build();
                return repo.updateTopupStatus(statusReq)
                    .compose(v -> Future.failedFuture(err));
              });
        })
        .compose((UpdateTopupContext ctx) -> repoSaldo.getSaldoByCardNumber(req.getCardNumber())
            .compose(saldoResp -> {
              int currentBalance = saldoResp.getData().getTotalBalance();
              int newBalance = currentBalance + ctx.topupDifference;

              span.addEvent("saldo_update_start");
              span.setAttribute("saldo.old_balance", (long) currentBalance);
              span.setAttribute("saldo.new_balance", (long) newBalance);

              return repoSaldo.updateSaldoBalance(req.getCardNumber(), newBalance)
                  .map(v -> {
                    span.addEvent("saldo_updated");
                    return ctx;
                  })
                  .recover(err -> {
                    span.recordException(Objects.requireNonNull(err));
                    span.addEvent("saldo_update_failed_rolling_back");
                    UpdateTopupAmount amountReq = UpdateTopupAmount.builder()
                        .topupId(req.getTopupId())
                        .topupAmount(ctx.existingTopup.getTopupAmount().intValue())
                        .build();
                    UpdateTopupStatus statusReq = UpdateTopupStatus.builder()
                        .topupId(req.getTopupId())
                        .status("failed")
                        .build();
                    return repo.updateTopupAmount(amountReq)
                        .compose(v -> repo.updateTopupStatus(statusReq))
                        .compose(v -> Future.failedFuture(err));
                  });
            }))
        .compose((UpdateTopupContext ctx) -> {
          span.addEvent("topup_mark_success");
          UpdateTopupStatus statusReq = UpdateTopupStatus.builder()
              .topupId(req.getTopupId())
              .status("success")
              .build();
          return repo.updateTopupStatus(statusReq)
              .compose(v -> repoQuery.getTopupById(req.getTopupId()));
        })
        .compose((Topup topup) -> {
          span.addEvent("delete_cache");
          String cacheKey = CACHE_PREFIX + req.getTopupId();
          return redisService.delete(cacheKey).map(v -> topup).recover(err -> {
            logger.warn("Failed to delete cache for topupId: {}", req.getTopupId(), err);
            return Future.succeededFuture(topup);
          });
        })
        .map((Topup topup) -> {
          tracingMetrics.completeSpanSuccess(tracingContext, "update", "Topup updated successfully");
          logger.info("Topup updated successfully. topupId={}, card={}", req.getTopupId(), req.getCardNumber());
          return ApiResponse.success("Topup updated successfully", TopupResponse.from(topup));
        })
        .recover(err -> {
          logger.error("Failed to update topup for card: {}, topupId: {}", req.getCardNumber(), req.getTopupId(), err);
          span.recordException(Objects.requireNonNull(err));
          tracingMetrics.completeSpanError(tracingContext, "update", err.getMessage());
          return Future.succeededFuture(ApiResponse.error("Failed to update topup: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<TopupResponseDeleteAt>> trashTopup(Integer topupId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TopupCommandService.trashTopup",
        Attributes.builder()
            .put("topup.id", (long) topupId)
            .build());

    logger.info("Trashing topup: {}", topupId);

    return repo.trashTopup(topupId)
        .compose(topup -> {
          if (topup == null) {
            return Future.failedFuture(new NotFoundException("Topup not found with id: " + topupId));
          }
          String cacheKey = CACHE_PREFIX + topupId;
          return redisService.delete(cacheKey)
              .onSuccess(deleted -> {
                if (deleted > 0) {
                  logger.debug("Topup {} cache invalidated on trash", topupId);
                }
              })
              .onFailure(
                  err -> logger.warn("Failed to invalidate cache for trashed topup {}: {}", topupId, err.getMessage()))
              .map(topup);
        })
        .map(topup -> {
          tracingMetrics.completeSpanSuccess(tracingContext, "trashed", "Topup trashed successfully");
          return ApiResponse.success("Topup trashed successfully", TopupResponseDeleteAt.from(topup));
        })
        .recover(err -> {
          logger.error("Failed to trash topup: {}", topupId, err);
          tracingMetrics.completeSpanError(tracingContext, "trashed", err.getMessage());
          return Future.succeededFuture(
              ApiResponse.error("Failed to trash topup: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<TopupResponseDeleteAt>> restoreTopup(Integer topupId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TopupCommandService.restoreTopup",
        Attributes.builder()
            .put("topup.id", (long) topupId)
            .build());

    logger.info("Restoring topup: {}", topupId);

    return repo.restoreTopup(topupId)
        .compose(topup -> {
          if (topup == null) {
            return Future.failedFuture(new NotFoundException("Topup not found with id: " + topupId));
          }
          String cacheKey = CACHE_PREFIX + topupId;
          return redisService.delete(cacheKey)
              .onSuccess(deleted -> {
                if (deleted > 0) {
                  logger.debug("Topup {} cache invalidated on restore", topupId);
                }
              })
              .onFailure(
                  err -> logger.warn("Failed to invalidate cache for restored topup {}: {}", topupId, err.getMessage()))
              .map(topup);
        })
        .map(topup -> {
          tracingMetrics.completeSpanSuccess(tracingContext, "restore", "Topup restored successfully");
          return ApiResponse.success(
              "Topup restored successfully",
              TopupResponseDeleteAt.from(topup));
        })
        .recover(err -> {
          logger.error("Failed to restore topup: {}", topupId, err);
          tracingMetrics.completeSpanError(tracingContext, "restore", err.getMessage());
          return Future.succeededFuture(
              ApiResponse.error("Failed to restore topup: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<Void>> deleteTopupPermanently(Integer topupId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TopupCommandService.deleteTopupPermanently",
        Attributes.builder()
            .put("topup.id", (long) topupId)
            .build());

    logger.info("Permanently deleting topup: {}", topupId);

    return repo.deleteTopupPermanently(topupId)
        .compose(v -> {
          String cacheKey = CACHE_PREFIX + topupId;
          return redisService.delete(cacheKey)
              .onSuccess(deleted -> {
                if (deleted > 0) {
                  logger.debug("Topup {} cache invalidated on permanent delete", topupId);
                }
              })
              .onFailure(
                  err -> logger.warn("Failed to invalidate cache for deleted topup {}: {}", topupId, err.getMessage()))
              .map(v);
        })
        .map(v -> {
          logger.info("Topup deleted successfully: {}", topupId);
          tracingMetrics.completeSpanSuccess(tracingContext, "deletePermanent", "Topup deleted permanently");
          return ApiResponse.<Void>success("success", null);
        })
        .recover(throwable -> {
          logger.error("Failed to deletePermanent topup: {}", topupId, throwable);
          tracingMetrics.completeSpanError(tracingContext, "deletePermanent", throwable.getMessage());
          return Future.succeededFuture(
              ApiResponse.error("Failed to delete topup: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<Void>> restoreAllTopups() {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("TopupCommandService.restoreAll");

    logger.info("Attempting to restore all trashed topups");

    return repo.restoreAllTopups()
        .compose(v -> {
          logger.info("All topups restored successfully");
          tracingMetrics.completeSpanSuccess(
              tracingContext,
              "restore_all",
              "All topups restored");
          return Future.succeededFuture(
              ApiResponse.<Void>success("All topups restored successfully"));
        })
        .recover(throwable -> {
          logger.error("Failed to restore all topups", throwable);
          tracingMetrics.completeSpanError(
              tracingContext,
              "restore_all",
              throwable.getMessage());
          return Future.succeededFuture(
              ApiResponse.error("Failed to restore all topups: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<Void>> deleteAllPermanentTopups() {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("TopupCommandService.deleteAllPermanent");

    logger.info("Attempting to permanently delete all trashed topups");

    return repo.deleteAllPermanentTopups()
        .compose(v -> {
          logger.info("All trashed topups permanently deleted");
          tracingMetrics.completeSpanSuccess(
              tracingContext,
              "deleteAllPermanent",
              "All topups permanently deleted");
          return Future.succeededFuture(
              ApiResponse.<Void>success("All topups permanently deleted"));
        })
        .recover(throwable -> {
          logger.error("Failed to permanently delete all topups", throwable);
          tracingMetrics.completeSpanError(
              tracingContext,
              "deleteAllPermanent",
              throwable.getMessage());
          return Future.succeededFuture(
              ApiResponse.error("Failed to permanently delete all topups: " + throwable.getMessage()));
        });
  }

  private static class CardTopupContext {
    final CardWithEmailResponse card;
    final Topup topup;

    CardTopupContext(CardWithEmailResponse card, Topup topup) {
      this.card = card;
      this.topup = topup;
    }
  }

  private static class UpdateTopupContext {
    final Topup existingTopup;
    int topupDifference;

    UpdateTopupContext(Topup existingTopup) {
      this.existingTopup = existingTopup;
    }
  }
}
