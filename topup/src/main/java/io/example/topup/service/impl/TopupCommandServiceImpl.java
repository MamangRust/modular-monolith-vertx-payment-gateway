package io.example.topup.service.impl;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
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
import lombok.RequiredArgsConstructor;
import pb.card.Card.CardWithEmailResponse;
import pb.card.CardCommand.UpdateCardRequest;

@RequiredArgsConstructor
public class TopupCommandServiceImpl implements TopupCommandService {
  private static final Logger logger = LoggerFactory.getLogger(TopupCommandServiceImpl.class);
  private static final String CACHE_PREFIX = "topup:";

  private final TopupCommandRepository repo;
  private final TopupQueryRepository repoQuery;
  private final CardClientRepository repoCard;
  private final SaldoClientRepository repoSaldo;
  private final RedisService redisService;
  private final TracingMetrics tracingMetrics;
  private final KafkaService kafkaService;

  private Future<Void> invalidateCache(Integer topupId) {
    return redisService.delete(CACHE_PREFIX + topupId)
        .compose(v -> redisService.delete(CACHE_PREFIX + "list:*"))
        .<Void>mapEmpty();
  }

  private Future<Void> invalidateListCache() {
    return redisService.delete(CACHE_PREFIX + "list:*").<Void>mapEmpty();
  }

  private Future<Void> sendTopupEmail(String email, int amount, Integer topupId) {
    if (kafkaService == null || email == null || email.isEmpty()) {
      return Future.succeededFuture();
    }

    String htmlBody = EmailTemplate.generateHtml(Map.of(
        "Title", "Topup Successful",
        "Message", String.format("Your topup of %d has been processed successfully.", amount),
        "Button", "View History",
        "Link", "https://sanedge.example.com/topup/history"));

    JsonObject emailPayload = new JsonObject()
        .put("email", email)
        .put("subject", "Topup Successful - SanEdge")
        .put("body", htmlBody);

    return kafkaService.sendMessage("email-service-topic-topup-create", String.valueOf(topupId), emailPayload)
        .<Void>mapEmpty()
        .onFailure(err -> logger.error("Failed to send topup email via Kafka for topupId: {}", topupId, err))
        .recover(err -> Future.succeededFuture());
  }

  private Future<CardTopupContext> markTopupFailed(CardTopupContext ctx, Throwable err) {
    UpdateTopupStatus statusReq = UpdateTopupStatus.builder()
        .topupId(ctx.topup.getId())
        .status("failed")
        .build();
    return repo.updateTopupStatus(statusReq)
        .compose(v -> Future.<CardTopupContext>failedFuture(err));
  }

  private Future<UpdateTopupContext> markTopupFailed(UpdateTopupContext ctx, Integer topupId, Throwable err) {
    UpdateTopupStatus statusReq = UpdateTopupStatus.builder()
        .topupId(topupId)
        .status("failed")
        .build();
    return repo.updateTopupStatus(statusReq)
        .compose(v -> Future.<UpdateTopupContext>failedFuture(err));
  }

  // ── Core Methods ────────────────────────────────────────────────

  @Override
  public Future<TopupResponse> createTopup(CreateTopupRequest req) {
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

          return repo.createTopup(req)
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
              return markTopupFailed(ctx, err);
            }))
        .compose((CardTopupContext ctx) -> {
          try {
            span.addEvent("card_update_start");
            CardWithEmailResponse card = ctx.card;

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
                  return markTopupFailed(ctx, err);
                });
          } catch (Exception e) {
            span.recordException(e);
            span.addEvent("card_update_exception");
            return markTopupFailed(ctx, e);
          }
        })
        .compose((CardTopupContext ctx) -> {
          span.addEvent("topup_mark_success");
          UpdateTopupStatus statusReq = UpdateTopupStatus.builder()
              .topupId(ctx.topup.getId())
              .status("success")
              .build();
          return repo.updateTopupStatus(statusReq).map(v -> ctx);
        })
        .compose((CardTopupContext ctx) -> sendTopupEmail(ctx.card.getEmail(), req.getTopupAmount(), ctx.topup.getId())
            .map(v -> ctx))
        .compose((CardTopupContext ctx) -> invalidateListCache().map(v -> ctx))
        .map(ctx -> TopupResponse.from(ctx.topup))
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "create", "Topup created successfully"))
        .onFailure(err -> {
          logger.error("Failed to create topup for card {}", req.getCardNumber(), err);
          tracingMetrics.completeSpanError(tracingContext, "create", err.getMessage());
        });
  }

  @Override
  public Future<TopupResponse> updateTopup(UpdateTopupRequest req) {
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
          return markTopupFailed(new UpdateTopupContext(null), req.getTopupId(), err);
        })
        .compose((UpdateTopupContext ctx) -> {
          int topupDifference = req.getTopupAmount() - ctx.existingTopup.getTopupAmount().intValue();
          span.addEvent("topup_update_start");
          span.setAttribute("topup.difference", (long) topupDifference);

          return repo.updateTopup(req)
              .map(v -> {
                span.addEvent("topup_updated");
                ctx.topupDifference = topupDifference;
                return ctx;
              })
              .recover(err -> {
                span.recordException(Objects.requireNonNull(err));
                span.addEvent("topup_update_failed");
                return markTopupFailed(ctx, req.getTopupId(), err);
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

                    return repo.updateTopupAmount(amountReq)
                        .compose(v -> markTopupFailed(ctx, req.getTopupId(), err));
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
        .compose((Topup topup) -> invalidateCache(req.getTopupId()).map(v -> topup))
        .map(TopupResponse::from)
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "update", "Topup updated successfully"))
        .onFailure(err -> {
          logger.error("Failed to update topup for card: {}, topupId: {}", req.getCardNumber(), req.getTopupId(), err);
          tracingMetrics.completeSpanError(tracingContext, "update", err.getMessage());
        });
  }

  @Override
  public Future<TopupResponseDeleteAt> trashTopup(Integer topupId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TopupCommandService.trashTopup",
        Attributes.builder().put("topup.id", (long) topupId).build());

    return repo.trashTopup(topupId)
        .compose(topup -> {
          if (topup == null) {
            return Future.<Topup>failedFuture(new NotFoundException("Topup not found with id: " + topupId));
          }
          return invalidateCache(topupId).<Topup>map(v -> topup);
        })
        .map(TopupResponseDeleteAt::from)
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "trashed", "Topup trashed successfully"))
        .onFailure(err -> {
          logger.error("Failed to trash topup: {}", topupId, err);
          tracingMetrics.completeSpanError(tracingContext, "trashed", err.getMessage());
        });
  }

  @Override
  public Future<TopupResponseDeleteAt> restoreTopup(Integer topupId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TopupCommandService.restoreTopup",
        Attributes.builder().put("topup.id", (long) topupId).build());

    return repoQuery.findByTrashed(topupId)
        .compose(trashed -> {
          if (trashed == null)
            return Future.failedFuture(new BadRequestException("Topup not found or must be trashed first"));
          return repo.restoreTopup(topupId);
        })
        .compose(topup -> {
          if (topup == null) {
            return Future.<Topup>failedFuture(new NotFoundException("Topup not found with id: " + topupId));
          }
          return invalidateCache(topupId).<Topup>map(v -> topup);
        })
        .map(TopupResponseDeleteAt::from)
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "restore", "Topup restored successfully"))
        .onFailure(err -> {
          logger.error("Failed to restore topup: {}", topupId, err);
          tracingMetrics.completeSpanError(tracingContext, "restore", err.getMessage());
        });
  }

  @Override
  public Future<Void> deleteTopupPermanently(Integer topupId) {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("TopupService.deletePermanent",
        Attributes.builder().put("topup.id", (long) topupId).build());

    return repoQuery.findByTrashed(topupId)
        .compose(trashed -> {
          if (trashed == null) {
            return Future.<Void>failedFuture(
                new BadRequestException("Topup not found or must be trashed first"));
          }
          return repo.deleteTopupPermanently(topupId)
              .compose(deleted -> {
                if (!deleted) {
                  return Future.<Void>failedFuture(
                      new BadRequestException("Topup not found or must be trashed first"));
                }
                return invalidateCache(topupId);
              });
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx,
            "deletePermanent", "Topup deleted permanently"))
        .onFailure(err -> {
          logger.error("Failed to deletePermanent topup: {}", topupId, err);
          tracingMetrics.completeSpanError(ctx, "deletePermanent", err.getMessage());
        });
  }

  @Override
  public Future<Void> restoreAllTopups() {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("TopupService.restoreAll");

    return repo.restoreAllTopups()
        .compose(count -> {
          if (count == 0) {
            return Future.<Void>failedFuture(new NotFoundException("No trashed topups found"));
          }
          return invalidateListCache();
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "restore_all", "All topups restored"))
        .onFailure(err -> {
          logger.error("Failed to restore all topups", err);
          tracingMetrics.completeSpanError(ctx, "restore_all", err.getMessage());
        });
  }

  @Override
  public Future<Void> deleteAllPermanentTopups() {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("TopupService.deleteAllPermanent");

    return repo.deleteAllPermanentTopups()
        .compose(count -> {
          if (count == 0) {
            return Future.<Void>failedFuture(new NotFoundException("No trashed topups found"));
          }
          return invalidateListCache();
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "deleteAllPermanent", "All topups permanently deleted"))
        .onFailure(err -> {
          logger.error("Failed to permanently delete all topups", err);
          tracingMetrics.completeSpanError(ctx, "deleteAllPermanent", err.getMessage());
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