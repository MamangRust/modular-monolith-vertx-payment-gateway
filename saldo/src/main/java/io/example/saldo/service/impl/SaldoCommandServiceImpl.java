package io.example.saldo.service.impl;

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
import io.example.saldo.model.Saldo;
import io.example.saldo.model.SaldoResponse;
import io.example.saldo.model.SaldoResponseDeleteAt;
import io.example.saldo.repository.CardClientRepository;
import io.example.saldo.repository.SaldoCommandRepository;
import io.example.saldo.repository.SaldoQueryRepository;
import io.example.saldo.service.SaldoCommandService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;
import io.example.saldo.domain.requests.CreateSaldoRequest;
import io.example.saldo.domain.requests.UpdateSaldoDeltaRequest;
import io.example.saldo.domain.requests.UpdateSaldoRequest;
import io.example.saldo.domain.requests.UpdateSaldoBalanceRequest;
import io.example.saldo.domain.requests.UpdateSaldoWithdrawRequest;

@RequiredArgsConstructor
public class SaldoCommandServiceImpl implements SaldoCommandService {
  private static final Logger logger = LoggerFactory.getLogger(SaldoCommandServiceImpl.class);
  private static final String CACHE_PREFIX = "saldo:";

  private final SaldoCommandRepository repo;
  private final SaldoQueryRepository queryRepository;
  private final CardClientRepository repoCard;
  private final RedisService redisService;
  private final KafkaService kafkaService;
  private final TracingMetrics tracingMetrics;

  private Future<Void> invalidateCache(Integer saldoId) {
    return redisService.delete(CACHE_PREFIX + saldoId)
        .compose(v -> redisService.deleteByPattern(CACHE_PREFIX + "all:*"))
        .compose(v -> redisService.deleteByPattern(CACHE_PREFIX + "active:*"))
        .compose(v -> redisService.deleteByPattern(CACHE_PREFIX + "stats:*"))
        .<Void>mapEmpty();
  }

  private Future<Void> invalidateListCache() {
    return redisService.deleteByPattern(CACHE_PREFIX + "all:*")
        .compose(v -> redisService.deleteByPattern(CACHE_PREFIX + "active:*"))
        .compose(v -> redisService.deleteByPattern(CACHE_PREFIX + "stats:*"))
        .<Void>mapEmpty();
  }

  private Future<Void> sendEmailNotification(String email, String cardNumber, Integer saldoId) {
    if (kafkaService == null || email == null || email.isEmpty()) {
      return Future.succeededFuture();
    }

    String htmlBody = EmailTemplate.generateHtml(Map.of(
        "Title", "Saldo Account Created",
        "Message", String.format("Your saldo account for card %s has been created successfully.", cardNumber),
        "Button", "View Balance",
        "Link", "https://sanedge.example.com/saldo"));

    JsonObject emailPayload = new JsonObject()
        .put("email", email)
        .put("subject", "Saldo Account Created - SanEdge")
        .put("body", htmlBody);

    return kafkaService.sendMessage("email-service-topic-saldo-create", String.valueOf(saldoId), emailPayload)
        .onFailure(err -> logger.error("Failed to send saldo email via Kafka for saldoId: {}", saldoId, err))
        .recover(err -> Future.succeededFuture());
  }

  @Override
  public Future<SaldoResponse> createSaldo(CreateSaldoRequest req) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "SaldoCommandService.createSaldo",
        Attributes.builder()
            .put("saldo.cardNumber", Objects.requireNonNull(req.getCardNumber()))
            .build());
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    logger.info("Creating saldo for card: {}", req.getCardNumber());

    return repoCard.findUserCardByCardNumber(req.getCardNumber())
        .compose(card -> repo.createSaldo(req)
            .compose(created -> {
              span.setAttribute("saldo.id", (long) created.getId());
              return invalidateListCache()
                  .compose(v -> sendEmailNotification(card.getEmail(), req.getCardNumber(), created.getId()))
                  .<Saldo>map(v -> created);
            }))
        .map(SaldoResponse::from)
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "create", "Saldo created successfully"))
        .onFailure(err -> {
          logger.error("Failed to create saldo for card: {}", req.getCardNumber(), err);
          tracingMetrics.completeSpanError(tracingContext, "create", err.getMessage());
        });
  }

  @Override
  public Future<SaldoResponse> updateSaldo(UpdateSaldoRequest req) {
    Integer saldoId = req.getSaldoId();
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "SaldoCommandService.updateSaldo",
        Attributes.builder()
            .put("saldo.id", (long) saldoId)
            .put("saldo.cardNumber", Objects.requireNonNull(req.getCardNumber()))
            .build());

    logger.info("Updating saldo: {}, card: {}", saldoId, req.getCardNumber());

    // cardNumber is optional for updates — only validate when provided
    Future<Void> cardCheck = req.getCardNumber() != null && !req.getCardNumber().isEmpty()
        ? repoCard.getCardByCardNumber(req.getCardNumber()).map(c -> (Void) null)
        : Future.succeededFuture();
    return cardCheck
        .compose(v -> repo.updateSaldo(req))
        .compose(updated -> {
          if (updated == null) {
            return Future.<Saldo>failedFuture(new NotFoundException("Saldo not found"));
          }
          return invalidateCache(saldoId).<Saldo>map(v -> updated);
        })
        .map(SaldoResponse::from)
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "update", "Saldo updated successfully"))
        .onFailure(err -> {
          logger.error("Failed to update saldo: {}", saldoId, err);
          tracingMetrics.completeSpanError(tracingContext, "update", err.getMessage());
        });
  }

  @Override
  public Future<SaldoResponseDeleteAt> trashSaldo(Integer saldoId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "SaldoCommandService.trashSaldo",
        Attributes.builder().put("saldo.id", (long) saldoId).build());

    return repo.trash(saldoId)
        .compose(saldo -> {
          if (saldo == null) {
            return Future.<Saldo>failedFuture(new NotFoundException("Saldo not found with id: " + saldoId));
          }
          return invalidateCache(saldoId).<Saldo>map(v -> saldo);
        })
        .map(SaldoResponseDeleteAt::from)
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "trashed", "Saldo trashed successfully"))
        .onFailure(err -> {
          logger.error("Failed to trash saldo: {}", saldoId, err);
          tracingMetrics.completeSpanError(tracingContext, "trashed", err.getMessage());
        });
  }

  @Override
  public Future<SaldoResponse> updateSaldoBalance(UpdateSaldoBalanceRequest req) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "SaldoCommandService.updateSaldoBalance",
        Attributes.builder().put("saldo.cardNumber", Objects.requireNonNull(req.getCardNumber())).build());

    return repo.updateSaldoBalance(req)
        .compose(updated -> {
          if (updated == null) {
            return Future.<Saldo>failedFuture(new NotFoundException("Saldo not found"));
          }
          return invalidateCache(updated.getId()).<Saldo>map(v -> updated);
        })
        .map(SaldoResponse::from)
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "updateBalance",
            "Saldo balance updated successfully"))
        .onFailure(err -> {
          logger.error("Failed to update saldo balance for card: {}", req.getCardNumber(), err);
          tracingMetrics.completeSpanError(tracingContext, "updateBalance", err.getMessage());
        });
  }

  @Override
  public Future<SaldoResponse> updateSaldoDelta(UpdateSaldoDeltaRequest req) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "SaldoCommandService.updateSaldoDelta",
        Attributes.builder()
            .put("saldo.cardNumber", Objects.requireNonNull(req.getCardNumber()))
            .put("saldo.delta", req.getDelta())
            .build());

    return repo.updateSaldoDelta(req)
        .compose(updated -> {
          if (updated == null) {
            return Future.<Saldo>failedFuture(
                new BadRequestException("Saldo not found or insufficient balance"));
          }
          return invalidateCache(updated.getId()).<Saldo>map(v -> updated);
        })
        .map(SaldoResponse::from)
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "updateDelta",
            "Saldo delta applied successfully"))
        .onFailure(err -> {
          logger.error("Failed to apply saldo delta for card: {}", req.getCardNumber(), err);
          tracingMetrics.completeSpanError(tracingContext, "updateDelta", err.getMessage());
        });
  }

  @Override
  public Future<SaldoResponse> updateSaldoWithdraw(UpdateSaldoWithdrawRequest req) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "SaldoCommandService.updateSaldoWithdraw",
        Attributes.builder().put("saldo.cardNumber", Objects.requireNonNull(req.getCardNumber())).build());

    return repo.updateSaldoWithdraw(req)
        .compose(updated -> {
          if (updated == null) {
            return Future.<Saldo>failedFuture(new BadRequestException("Saldo not found or insufficient balance"));
          }
          return invalidateCache(updated.getId()).<Saldo>map(v -> updated);
        })
        .map(SaldoResponse::from)
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "updateWithdraw",
            "Saldo withdraw updated successfully"))
        .onFailure(err -> {
          logger.error("Failed to update saldo withdraw for card: {}", req.getCardNumber(), err);
          tracingMetrics.completeSpanError(tracingContext, "updateWithdraw", err.getMessage());
        });
  }

  @Override
  public Future<SaldoResponseDeleteAt> restoreSaldo(Integer saldoId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "SaldoCommandService.restoreSaldo",
        Attributes.builder().put("saldo.id", (long) saldoId).build());

    return queryRepository.findByTrashedId(saldoId)
        .compose(trashed -> {
          if (trashed == null)
            return Future.failedFuture(new BadRequestException("Saldo not found or must be trashed first"));
          return repo.restore(saldoId);
        })
        .compose(saldo -> {
          if (saldo == null) {
            return Future.<Saldo>failedFuture(new NotFoundException("Saldo not found with id: " + saldoId));
          }
          return invalidateCache(saldoId).<Saldo>map(v -> saldo);
        })
        .map(SaldoResponseDeleteAt::from)
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "restore", "Saldo restored successfully"))
        .onFailure(err -> {
          logger.error("Failed to restore saldo: {}", saldoId, err);
          tracingMetrics.completeSpanError(tracingContext, "restore", err.getMessage());
        });
  }

  @Override
  public Future<Void> deleteSaldoPermanently(Integer saldoId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "SaldoCommandService.deleteSaldoPermanently",
        Attributes.builder().put("saldo.id", (long) saldoId).build());

    return queryRepository.findByTrashedId(saldoId)
        .compose(trashed -> {
          if (trashed == null)
            return Future.failedFuture(new BadRequestException("Saldo not found or must be trashed first"));
          return repo.deletePermanent(saldoId);
        })
        .compose(deleted -> {
          if (!deleted)
            return Future.failedFuture(new BadRequestException("Saldo not found or must be trashed first"));
          return invalidateCache(saldoId).map(v -> (Void) null);
        })
        .onSuccess(
            v -> tracingMetrics.completeSpanSuccess(tracingContext, "deletePermanent", "Saldo deleted permanently"))
        .onFailure(err -> {
          logger.error("Failed to deletePermanent saldo: {}", saldoId, err);
          tracingMetrics.completeSpanError(tracingContext, "deletePermanent", err.getMessage());
        });
  }

  @Override
  public Future<Void> restoreAllSaldos() {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("SaldoCommandService.restoreAllSaldos");

    return repo.restoreAll()
        .compose(count -> {
          if (count == 0) {
            return Future.<Void>failedFuture(new NotFoundException("No trashed saldos found"));
          }
          return invalidateListCache();
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "restoreAllSaldos",
            "All saldos restored successfully"))
        .onFailure(err -> {
          logger.error("Failed to restore all saldos", err);
          tracingMetrics.completeSpanError(tracingContext, "restoreAllSaldos", err.getMessage());
        });
  }

  @Override
  public Future<Void> deleteAllPermanentSaldos() {
    TracingMetrics.TracingContext tracingContext = tracingMetrics
        .startSpan("SaldoCommandService.deleteAllPermanentSaldos");

    return repo.deleteAllPermanent()
        .compose(count -> {
          if (count == 0) {
            return Future.<Void>failedFuture(new NotFoundException("No trashed saldos found"));
          }
          return invalidateListCache();
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(tracingContext, "deleteAllPermanentSaldos",
            "All saldos permanently deleted"))
        .onFailure(err -> {
          logger.error("Failed to permanently delete all saldos", err);
          tracingMetrics.completeSpanError(tracingContext, "deleteAllPermanentSaldos", err.getMessage());
        });
  }
}