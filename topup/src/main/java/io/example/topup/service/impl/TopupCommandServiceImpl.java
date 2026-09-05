package io.example.topup.service.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.ConflictException;
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
        .compose(updated -> updated == null
            ? Future.<CardTopupContext>failedFuture(statusFailure(ctx.topup.getId(), "failed", err))
            : Future.<CardTopupContext>failedFuture(err))
        .recover(statusErr -> {
          if (statusErr == err) {
            return Future.failedFuture(err);
          }
          return markTopupCompensationRequired(ctx, err, statusErr);
        });
  }

  private Future<UpdateTopupContext> markTopupFailed(UpdateTopupContext ctx, Integer topupId, Throwable err) {
    UpdateTopupStatus statusReq = UpdateTopupStatus.builder()
        .topupId(topupId)
        .status("failed")
        .build();
    return repo.updateTopupStatus(statusReq)
        .compose(updated -> updated == null
            ? Future.<UpdateTopupContext>failedFuture(statusFailure(topupId, "failed", err))
            : Future.<UpdateTopupContext>failedFuture(err))
        .recover(statusErr -> {
          if (statusErr == err) {
            return Future.failedFuture(err);
          }
          return markTopupCompensationRequired(ctx, topupId, err, statusErr);
        });
  }

  private IllegalStateException statusFailure(Integer topupId, String status, Throwable cause) {
    IllegalStateException failure = new IllegalStateException(
        "Could not persist topup " + topupId + " status '" + status + "'");
    if (cause != null) {
      failure.addSuppressed(cause);
    }
    return failure;
  }

  private Future<CardTopupContext> markTopupCompensationRequired(CardTopupContext ctx,
      Throwable originalError, Throwable compensationError) {
    UpdateTopupStatus statusReq = UpdateTopupStatus.builder()
        .topupId(ctx.topup.getId())
        .status("compensation_required")
        .build();
    return repo.updateTopupStatus(statusReq)
        .compose(updated -> {
          Throwable failure = new IllegalStateException(
              "Topup compensation could not be completed for " + ctx.topup.getId());
          failure.addSuppressed(originalError);
          failure.addSuppressed(compensationError);
          if (updated == null) {
            failure.addSuppressed(statusFailure(ctx.topup.getId(), "compensation_required", failure));
          }
          return Future.<CardTopupContext>failedFuture(failure);
        })
        .recover(statusError -> {
          statusError.addSuppressed(originalError);
          statusError.addSuppressed(compensationError);
          return Future.failedFuture(statusError);
        });
  }

  private Future<UpdateTopupContext> markTopupCompensationRequired(UpdateTopupContext ctx,
      Integer topupId, Throwable originalError, Throwable compensationError) {
    return repo.updateTopupStatus(UpdateTopupStatus.builder()
        .topupId(topupId)
        .status("compensation_required")
        .build())
        .compose(updated -> {
          IllegalStateException failure = new IllegalStateException(
              "Topup compensation could not be completed for " + topupId);
          failure.addSuppressed(originalError);
          failure.addSuppressed(compensationError);
          if (updated == null) {
            failure.addSuppressed(statusFailure(topupId, "compensation_required", failure));
          }
          return Future.<UpdateTopupContext>failedFuture(failure);
        })
        .recover(statusError -> {
          statusError.addSuppressed(originalError);
          statusError.addSuppressed(compensationError);
          return Future.failedFuture(statusError);
        });
  }

  private Future<CardTopupContext> compensateTopupAndFail(CardTopupContext ctx, String cardNumber,
      int amount, Throwable originalError) {
    // Scope recovery to the inverse saldo operation only. The intentional
    // failed future returned by markTopupFailed must not be mistaken for a
    // compensation failure.
    return repoSaldo.updateSaldoDelta(cardNumber, -amount)
        .<Void>mapEmpty()
        .recover(compensationError -> markTopupCompensationRequired(ctx, originalError, compensationError)
            .<Void>mapEmpty())
        .compose(ignored -> markTopupFailed(ctx, originalError));
  }

  private Future<UpdateTopupContext> rollbackAmountAndFail(UpdateTopupContext ctx, Integer topupId,
      Throwable originalError) {
    UpdateTopupAmount amountReq = UpdateTopupAmount.builder()
        .topupId(topupId)
        .topupAmount(ctx.existingTopup.getTopupAmount().intValue())
        .build();

    return repo.updateTopupAmount(amountReq)
        .<Void>mapEmpty()
        .recover(compensationError -> markTopupCompensationRequired(ctx, topupId, originalError, compensationError)
            .<Void>mapEmpty())
        .compose(ignored -> markTopupFailed(ctx, topupId, originalError));
  }

  private Future<Void> reverseTopupSaldo(String cardNumber, int delta) {
    if (delta == 0) {
      return Future.succeededFuture();
    }
    return repoSaldo.updateSaldoDelta(cardNumber, -delta).<Void>mapEmpty();
  }

  private Future<Topup> compensateUpdatedTopup(UpdateTopupContext ctx, UpdateTopupRequest req,
      Throwable originalError) {
    UpdateTopupAmount amountReq = UpdateTopupAmount.builder()
        .topupId(req.getTopupId())
        .topupAmount(ctx.existingTopup.getTopupAmount().intValue())
        .build();

    return reverseTopupSaldo(req.getCardNumber(), ctx.topupDifference)
        .compose(ignored -> repo.updateTopupAmount(amountReq).<Void>mapEmpty())
        .recover(compensationError -> markTopupCompensationRequired(ctx, req.getTopupId(), originalError,
            compensationError).<Void>mapEmpty())
        .compose(ignored -> persistTopupStatus(req.getTopupId(), "failed")
            .recover(statusError -> markTopupCompensationRequired(ctx, req.getTopupId(), originalError,
                statusError).mapEmpty()))
        .compose(ignored -> Future.<Topup>failedFuture(originalError));
  }

  private Future<Topup> markTopupStatusAmbiguous(UpdateTopupContext ctx, UpdateTopupRequest req,
      Throwable statusError) {
    return repo.updateTopupStatus(UpdateTopupStatus.builder()
        .topupId(req.getTopupId())
        .status("compensation_required")
        .build())
        .compose(updated -> {
          IllegalStateException failure = new IllegalStateException(
              "Topup success status is ambiguous for " + req.getTopupId());
          failure.addSuppressed(statusError);
          if (updated == null) {
            failure.addSuppressed(statusFailure(req.getTopupId(), "compensation_required", failure));
          }
          return Future.<Topup>failedFuture(failure);
        })
        .recover(markerError -> {
          markerError.addSuppressed(statusError);
          return Future.failedFuture(markerError);
        });
  }

  private Future<Topup> persistTopupStatus(Integer topupId, String status) {
    return repo.updateTopupStatus(UpdateTopupStatus.builder()
        .topupId(topupId)
        .status(status)
        .build())
        .compose(updated -> updated == null
            ? Future.<Topup>failedFuture(statusFailure(topupId, status, null))
            : Future.succeededFuture(updated));
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

    // Idempotency guard: a retried request with the same key returns the
    // original result instead of double-crediting the balance.
    String idempotencyKey = req.getIdempotencyKey();
    Future<CardTopupContext> idempotencyCheck = idempotencyKey != null && !idempotencyKey.isBlank()
        ? repo.findByIdempotencyKey(idempotencyKey)
            .compose(existing -> existing == null
                ? Future.<CardTopupContext>succeededFuture(null)
                : replayOrReject(existing, req))
        : Future.succeededFuture();

    return idempotencyCheck
        .compose(replayed -> {
          if (replayed != null) {
            span.addEvent("idempotent_replay");
            span.setAttribute("topup.id", (long) replayed.topup.getId());
            logger.info("Replaying existing topup {} for idempotency key {}", replayed.topup.getId(), idempotencyKey);
            return Future.<CardTopupContext>succeededFuture(replayed);
          }

          return repoCard.getCardEmailByCardNumber(req.getCardNumber())
              .compose((CardWithEmailResponse card) -> {
                span.addEvent("card_found");
                span.setAttribute("card.id", (long) card.getId());

                return repo.createTopup(req)
                    .compose((Topup topup) -> {
                      if (topup != null) {
                        span.addEvent("topup_created");
                        span.setAttribute("topup.id", (long) topup.getId());
                        return Future.succeededFuture(new CardTopupContext(card, topup));
                      }
                      if (idempotencyKey == null || idempotencyKey.isBlank()) {
                        return Future.<CardTopupContext>failedFuture(
                            new IllegalStateException("Topup insert returned no row"));
                      }
                      // A concurrent request won the unique idempotency key.
                      // Re-read it and replay only a terminal successful result;
                      // never double-credit a pending/failed reservation.
                      return repo.findByIdempotencyKey(idempotencyKey)
                          .compose(raced -> replayOrReject(raced, req));
                    });
              })
              // A raced request may have returned a terminal row. It must
              // bypass all mutation steps just like the initial replay path.
              .compose((CardTopupContext ctx) -> {
                if (ctx.card == null) {
                  return Future.succeededFuture(ctx);
                }
                span.addEvent("saldo_credit_start");
                return repoSaldo.updateSaldoDelta(req.getCardNumber(), req.getTopupAmount())
                    .map(v -> {
                      span.addEvent("saldo_credited");
                      return ctx;
                    })
                    .recover(err -> {
                      span.recordException(Objects.requireNonNull(err));
                      span.addEvent("saldo_credit_failed");
                      return markTopupFailed(ctx, err);
                    });
              })
              .compose((CardTopupContext ctx) -> updateCardAndCompensate(ctx, span, req.getTopupAmount()));
        })
        .compose((CardTopupContext ctx) -> {
          // Skip the rest of the happy path on idempotent replay.
          if (ctx.card == null) {
            return Future.succeededFuture(ctx);
          }
          span.addEvent("topup_mark_success");
          UpdateTopupStatus statusReq = UpdateTopupStatus.builder()
              .topupId(ctx.topup.getId())
              .status("success")
              .build();
          return repo.updateTopupStatus(statusReq)
              .compose(updated -> {
                if (updated == null) {
                  return Future.<CardTopupContext>failedFuture(
                      new IllegalStateException("Topup status update returned no row"));
                }
                // Keep the response/cache model in sync with the persisted
                // terminal state; the INSERT row was created as pending.
                ctx.topup.setStatus("success");
                return Future.succeededFuture(ctx);
              })
              // Balance was already credited. If finalization fails, reverse
              // it and retain the original status-update error.
              .recover(err -> compensateTopupAndFail(ctx, req.getCardNumber(), req.getTopupAmount(), err));
        })
        .compose((CardTopupContext ctx) -> {
          if (ctx.card == null) {
            return Future.succeededFuture(ctx);
          }
          return sendTopupEmail(ctx.card.getEmail(), req.getTopupAmount(), ctx.topup.getId())
              .map(v -> ctx);
        })
        .compose((CardTopupContext ctx) -> {
          if (ctx.card == null) {
            return invalidateCache(ctx.topup.getId()).map(v -> ctx);
          }
          return invalidateListCache().map(v -> ctx);
        })
        .map(ctx -> TopupResponse.from(ctx.topup))
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "create", "Topup created successfully"))
        .onFailure(err -> {
          logger.error("Failed to create topup for card {}", req.getCardNumber(), err);
          tracingMetrics.completeSpanError(tracingContext, "create", err.getMessage());
        });
  }

  private Future<CardTopupContext> replayOrReject(Topup existing, CreateTopupRequest req) {
    if (!sameTopupRequest(existing, req)) {
      return Future.failedFuture(new ConflictException(
          "Idempotency key was already used for a different topup"));
    }
    if (!"success".equalsIgnoreCase(existing.getStatus())) {
      String status = existing.getStatus() == null ? "unknown" : existing.getStatus();
      return Future.failedFuture(new ConflictException(
          "Topup with this idempotency key is not replayable (status: " + status + ")"));
    }
    return Future.succeededFuture(new CardTopupContext(null, existing));
  }

  private boolean sameTopupRequest(Topup existing, CreateTopupRequest req) {
    // topup_no is DB-generated (gen_random_uuid) when the request omits it, so a
    // blank request value must act as a wildcard here — otherwise legitimate
    // idempotent retries would compare a generated UUID against "" and fail.
    boolean topupNoMatches = req.getTopupNo() == null || req.getTopupNo().isBlank()
        || Objects.equals(existing.getTopupNo(), req.getTopupNo());
    return existing != null
        && Objects.equals(existing.getCardNumber(), req.getCardNumber())
        && topupNoMatches
        && Objects.equals(existing.getTopupAmount(), (long) req.getTopupAmount())
        && Objects.equals(existing.getTopupMethod(), req.getTopupMethod());
  }

  /**
   * Touches the card after the balance credit. If that fails, the credit is
   * compensated with an inverse delta so the balance cannot be left changed
   * while the topup is marked failed (SUPERPLANNING B.7 #3).
   */
  private Future<CardTopupContext> updateCardAndCompensate(CardTopupContext ctx, Span span, int creditedAmount) {
    // A concurrent insert that lost the idempotency race is represented by a
    // context without a card. It is already a terminal replay and must not
    // continue into card or saldo mutation steps.
    if (ctx.card == null) {
      return Future.succeededFuture(ctx);
    }

    try {
      span.addEvent("card_update_start");
      CardWithEmailResponse card = ctx.card;

      // expire_date is DATE-typed in Postgres and surfaces as a date-only
      // string (e.g. "2035-12-30"); Instant.parse requires a full ISO instant,
      // so fall back to LocalDate for date-only values.
      Instant expireInstant = parseExpireDate(card.getExpireDate());
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
            span.addEvent("card_update_failed_compensating");
            // Reverse the credit so balance is restored.
            return compensateTopupAndFail(ctx, card.getCardNumber(), creditedAmount, err);
          });
    } catch (Exception e) {
      span.recordException(e);
      span.addEvent("card_update_exception_compensating");
      return compensateTopupAndFail(ctx, ctx.card.getCardNumber(), creditedAmount, e);
    }
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
              .compose(updated -> {
                if (updated == null) {
                  return Future.<UpdateTopupContext>failedFuture(
                      new IllegalStateException("Topup update returned no row"));
                }
                span.addEvent("topup_updated");
                ctx.topupDifference = topupDifference;
                return Future.succeededFuture(ctx);
              })
              .recover(err -> {
                span.recordException(Objects.requireNonNull(err));
                span.addEvent("topup_update_failed");
                return markTopupFailed(ctx, req.getTopupId(), err);
              });
        })
        .compose((UpdateTopupContext ctx) -> {
          span.addEvent("saldo_delta_start");
          span.setAttribute("topup.difference", (long) ctx.topupDifference);

          // Atomic adjustment by difference (no read-modify-write). A negative
          // difference (reducing the amount) is a guarded debit.
          return repoSaldo.updateSaldoDelta(req.getCardNumber(), ctx.topupDifference)
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

                return rollbackAmountAndFail(ctx, req.getTopupId(), err);
              });
        })
        .compose((UpdateTopupContext ctx) -> {
          span.addEvent("topup_mark_success");
          UpdateTopupStatus statusReq = UpdateTopupStatus.builder()
              .topupId(req.getTopupId())
              .status("success")
              .build();
          return repo.updateTopupStatus(statusReq)
              .recover(statusErr -> markTopupStatusAmbiguous(ctx, req, statusErr))
              .compose(updated -> {
                if (updated == null) {
                  return markTopupStatusAmbiguous(ctx, req,
                      new IllegalStateException("Topup status update returned no row"));
                }
                // The status update has committed. A read-back failure is
                // observational only; do not reverse a committed financial update.
                return repoQuery.getTopupById(req.getTopupId())
                    .compose(readBack -> readBack == null
                        ? Future.<Topup>failedFuture(new IllegalStateException(
                            "Topup status committed but read-back returned no row"))
                        : Future.succeededFuture(readBack));
              });
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

  /**
   * Parses an expire-date value that may be a full ISO instant ("2035-12-30T00:00:00Z")
   * or a date-only string ("2035-12-30").
   */
  private static Instant parseExpireDate(String value) {
    if (value == null || value.isBlank()) {
      return Instant.now();
    }
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException e) {
      return LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
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