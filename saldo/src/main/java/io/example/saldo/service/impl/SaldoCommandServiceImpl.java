package io.example.saldo.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Objects;

import io.example.common.exception.NotFoundException;
import io.example.common.model.ApiResponse;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.saldo.model.Saldo;
import io.example.saldo.model.SaldoResponse;
import io.example.saldo.model.SaldoResponseDeleteAt;
import io.example.saldo.repository.CardClientRepository;
import io.example.saldo.repository.SaldoCommandRepository;
import io.example.saldo.service.SaldoCommandService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import io.example.saldo.domain.requests.CreateSaldoRequest;
import io.example.saldo.domain.requests.UpdateSaldoRequest;
import io.example.saldo.domain.requests.UpdateSaldoBalanceRequest;
import io.example.saldo.domain.requests.UpdateSaldoWithdrawRequest;

public class SaldoCommandServiceImpl implements SaldoCommandService {
  private static final Logger logger = LoggerFactory.getLogger(SaldoCommandServiceImpl.class);

  private final SaldoCommandRepository repo;
  private final CardClientRepository repoCard;
  private final RedisService redisService;
  private final TracingMetrics tracingMetrics;

  private static final String CACHE_PREFIX = "saldo:";

  public SaldoCommandServiceImpl(
      SaldoCommandRepository repo,
      CardClientRepository repoCard,
      RedisService redisService,
      TracingMetrics tracingMetrics) {
    this.repo = repo;
    this.repoCard = repoCard;
    this.redisService = redisService;
    this.tracingMetrics = tracingMetrics;
  }

  @Override
  public Future<ApiResponse<SaldoResponse>> createSaldo(CreateSaldoRequest req) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "SaldoCommandService.createSaldo",
        Attributes.builder()
            .put("saldo.cardNumber", Objects.requireNonNull(req.getCardNumber()))
            .build());
    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    logger.info("Creating saldo for card: {}", req.getCardNumber());

    return repoCard.getCardByCardNumber(req.getCardNumber())
        .compose(card -> {
          logger.debug("Card {} validated via gRPC, proceeding with saldo creation.", req.getCardNumber());
          return repo.createSaldo(req);
        })
        .compose(created -> {
          // Invalidate list and stats caches
          return redisService.deleteByPattern(CACHE_PREFIX + "all:*")
              .compose(v -> redisService.deleteByPattern(CACHE_PREFIX + "active:*"))
              .compose(v -> redisService.deleteByPattern(CACHE_PREFIX + "stats:*"))
              .map(v -> created);
        })
        .map(created -> {
          span.setAttribute("saldo.id", (long) created.getId());
          logger.info("Successfully created saldo record | card_number={}, amount={}", req.getCardNumber(),
              req.getTotalBalance());
          tracingMetrics.completeSpanSuccess(tracingContext, "create", "Saldo created successfully");
          return ApiResponse.success(
              "Saldo created successfully",
              SaldoResponse.from(created));
        })
        .recover(err -> {
          logger.error("Failed to create saldo for card: {}", req.getCardNumber(), err);
          tracingMetrics.completeSpanError(tracingContext, "create", err.getMessage());

          String errorMessage = "Failed to create saldo: " + err.getMessage();
          if (err instanceof NotFoundException) {
            errorMessage = "Failed to create saldo: Card number not found.";
          }

          return Future.succeededFuture(
              ApiResponse.<SaldoResponse>error(errorMessage));
        });
  }

  @Override
  public Future<ApiResponse<SaldoResponse>> updateSaldo(UpdateSaldoRequest req) {
    Integer saldoId = req.getSaldoId();
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "SaldoCommandService.updateSaldo",
        Attributes.builder()
            .put("saldo.id", (long) saldoId)
            .put("saldo.cardNumber", Objects.requireNonNull(req.getCardNumber()))
            .build());

    logger.info("Updating saldo: {}, card: {}", saldoId, req.getCardNumber());

    return repoCard.getCardByCardNumber(req.getCardNumber())
        .compose(card -> {
          logger.debug("Card {} validated via gRPC, proceeding with saldo update.", req.getCardNumber());
          return repo.updateSaldo(req);
        })
        .compose((Saldo updatedSaldo) -> {
          if (updatedSaldo == null) {
            return Future.failedFuture(new NotFoundException("Saldo not found"));
          }
          String cacheKey = CACHE_PREFIX + saldoId;
          return redisService.delete(cacheKey) // Specific cache
              .compose(v -> redisService.deleteByPattern(CACHE_PREFIX + "all:*"))
              .compose(v -> redisService.deleteByPattern(CACHE_PREFIX + "active:*"))
              .compose(v -> redisService.deleteByPattern(CACHE_PREFIX + "stats:*"))
              .map(updatedSaldo);
        })
        .map((Saldo updatedSaldo) -> {
          logger.info("Successfully updated saldo record | card_number={}, amount={}", req.getCardNumber(),
              req.getTotalBalance());
          tracingMetrics.completeSpanSuccess(tracingContext, "update", "Saldo updated successfully");
          return ApiResponse.success(
              "Saldo updated successfully",
              SaldoResponse.from(updatedSaldo));
        })
        .recover(err -> {
          logger.error("Failed to update saldo: {}", saldoId, err);
          tracingMetrics.completeSpanError(tracingContext, "update", err.getMessage());

          String errorMessage = "Failed to update saldo: " + err.getMessage();
          if (err instanceof NotFoundException) {
            errorMessage = "Failed to update saldo: Card or Saldo not found.";
          }

          return Future.succeededFuture(
              ApiResponse.<SaldoResponse>error(errorMessage));
        });
  }

  @Override
  public Future<ApiResponse<SaldoResponseDeleteAt>> trashSaldo(Integer saldoId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "SaldoCommandService.trashSaldo",
        Attributes.builder()
            .put("saldo.id", (long) saldoId)
            .build());

    logger.info("Trashing saldo: {}", saldoId);

    return repo.trash(saldoId)
        .compose(saldo -> {
          if (saldo == null) {
            return Future.failedFuture(new NotFoundException("Saldo not found with id: " + saldoId));
          }
          String cacheKey = CACHE_PREFIX + saldoId;
          return redisService.delete(cacheKey)
              .onSuccess(deleted -> {
                if (deleted > 0) {
                  logger.debug("Saldo {} cache invalidated on trash", saldoId);
                }
              })
              .onFailure(
                  err -> logger.warn("Failed to invalidate cache for trashed saldo {}: {}", saldoId, err.getMessage()))
              .map(saldo);
        })
        .map(saldo -> {
          tracingMetrics.completeSpanSuccess(tracingContext, "trashed", "Saldo trashed successfully");
          return ApiResponse.success("Saldo trashed successfully", SaldoResponseDeleteAt.from(saldo));
        })
        .recover(err -> {
          logger.error("Failed to trash saldo: {}", saldoId, err);
          tracingMetrics.completeSpanError(tracingContext, "trashed", err.getMessage());
          return Future.succeededFuture(
              ApiResponse.<SaldoResponseDeleteAt>error("Failed to trash saldo: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<SaldoResponse>> updateSaldoBalance(UpdateSaldoBalanceRequest req) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "SaldoCommandService.updateSaldoBalance",
        Attributes.builder()
            .put("saldo.cardNumber", Objects.requireNonNull(req.getCardNumber()))
            .build());

    logger.info("Updating saldo balance for card: {}", req.getCardNumber());

    return repo.updateSaldoBalance(req)
        .compose(updated -> {
          if (updated == null) {
            return Future.failedFuture(new NotFoundException("Saldo not found"));
          }
          String cacheKey = CACHE_PREFIX + updated.getId();
          return redisService.delete(cacheKey)
              .compose(v -> redisService.deleteByPattern(CACHE_PREFIX + "all:*"))
              .compose(v -> redisService.deleteByPattern(CACHE_PREFIX + "active:*"))
              .compose(v -> redisService.deleteByPattern(CACHE_PREFIX + "stats:*"))
              .map(updated);
        })
        .map(updated -> {
          logger.info("Successfully updated saldo record (balance) | card_number={}, amount={}", req.getCardNumber(),
              req.getTotalBalance());
          tracingMetrics.completeSpanSuccess(tracingContext, "updateBalance", "Saldo balance updated successfully");
          return ApiResponse.success("Saldo balance updated successfully", SaldoResponse.from(updated));
        })
        .recover(err -> {
          logger.error("Failed to update saldo balance for card: {}", req.getCardNumber(), err);
          tracingMetrics.completeSpanError(tracingContext, "updateBalance", err.getMessage());
          return Future
              .succeededFuture(ApiResponse.<SaldoResponse>error("Failed to update saldo balance: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<SaldoResponse>> updateSaldoWithdraw(UpdateSaldoWithdrawRequest req) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "SaldoCommandService.updateSaldoWithdraw",
        Attributes.builder()
            .put("saldo.cardNumber", Objects.requireNonNull(req.getCardNumber()))
            .build());

    logger.info("Updating saldo withdraw for card: {}", req.getCardNumber());

    return repo.updateSaldoWithdraw(req)
        .compose(updated -> {
          if (updated == null) {
            return Future.failedFuture(new NotFoundException("Saldo not found or insufficient balance"));
          }
          String cacheKey = CACHE_PREFIX + updated.getId();
          return redisService.delete(cacheKey)
              .compose(v -> redisService.deleteByPattern(CACHE_PREFIX + "all:*"))
              .compose(v -> redisService.deleteByPattern(CACHE_PREFIX + "active:*"))
              .compose(v -> redisService.deleteByPattern(CACHE_PREFIX + "stats:*"))
              .map(updated);
        })
        .map(updated -> {
          logger.info("Successfully updated saldo record (withdraw) | card_number={}, amount={}", req.getCardNumber(),
              req.getWithdrawAmount());
          tracingMetrics.completeSpanSuccess(tracingContext, "updateWithdraw", "Saldo withdraw updated successfully");
          return ApiResponse.success("Saldo withdraw updated successfully", SaldoResponse.from(updated));
        })
        .recover(err -> {
          logger.error("Failed to update saldo withdraw for card: {}", req.getCardNumber(), err);
          tracingMetrics.completeSpanError(tracingContext, "updateWithdraw", err.getMessage());
          return Future
              .succeededFuture(
                  ApiResponse.<SaldoResponse>error("Failed to update saldo withdraw: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<SaldoResponseDeleteAt>> restoreSaldo(Integer saldoId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "SaldoCommandService.restoreSaldo",
        Attributes.builder()
            .put("saldo.id", (long) saldoId)
            .build());

    logger.info("Restoring saldo: {}", saldoId);

    return repo.restore(saldoId)
        .compose(saldo -> {
          if (saldo == null) {
            return Future.failedFuture(new NotFoundException("Saldo not found with id: " + saldoId));
          }
          String cacheKey = CACHE_PREFIX + saldoId;
          return redisService.delete(cacheKey)
              .onSuccess(deleted -> {
                if (deleted > 0) {
                  logger.debug("Saldo {} cache invalidated on restore", saldoId);
                }
              })
              .onFailure(
                  err -> logger.warn("Failed to invalidate cache for restored saldo {}: {}", saldoId, err.getMessage()))
              .map(saldo);
        })
        .map(saldo -> {
          tracingMetrics.completeSpanSuccess(tracingContext, "restore", "Saldo restored successfully");
          return ApiResponse.success(
              "Saldo restored successfully",
              SaldoResponseDeleteAt.from(saldo));
        })
        .recover(err -> {
          logger.error("Failed to restore saldo: {}", saldoId, err);
          tracingMetrics.completeSpanError(tracingContext, "restore", err.getMessage());
          return Future.succeededFuture(
              ApiResponse.<SaldoResponseDeleteAt>error("Failed to restore saldo: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<Void>> deleteSaldoPermanently(Integer saldoId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "SaldoCommandService.deleteSaldoPermanently",
        Attributes.builder()
            .put("saldo.id", (long) saldoId)
            .build());

    logger.info("Permanently deleting saldo: {}", saldoId);

    return repo.deletePermanent(saldoId)
        .compose(v -> {
          String cacheKey = CACHE_PREFIX + saldoId;
          return redisService.delete(cacheKey)
              .onSuccess(deleted -> {
                if (deleted > 0) {
                  logger.debug("Saldo {} cache invalidated on permanent delete", saldoId);
                }
              })
              .onFailure(
                  err -> logger.warn("Failed to invalidate cache for deleted saldo {}: {}", saldoId, err.getMessage()))
              .map(v);
        })
        .map(v -> {
          logger.info("Saldo deleted successfully: {}", saldoId);
          tracingMetrics.completeSpanSuccess(tracingContext, "deletePermanent", "Saldo deleted permanently");
          return ApiResponse.<Void>success("success", null);
        })
        .recover(throwable -> {
          logger.error("Failed to deletePermanent saldo: {}", saldoId, throwable);
          tracingMetrics.completeSpanError(tracingContext, "deletePermanent", throwable.getMessage());
          return Future.succeededFuture(
              ApiResponse.<Void>error("Failed to delete saldo: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<Void>> restoreAllSaldos() {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("SaldoService.restoreAll");

    logger.info("Attempting to restore all trashed saldos");

    return repo.restoreAll()
        .compose(v -> {
          logger.info("All saldos restored successfully");
          tracingMetrics.completeSpanSuccess(
              tracingContext,
              "restore_all",
              "All saldos restored");
          return Future.succeededFuture(
              ApiResponse.<Void>success("All saldos restored successfully"));
        })
        .recover(throwable -> {
          logger.error("Failed to restore all saldos", throwable);
          tracingMetrics.completeSpanError(
              tracingContext,
              "restore_all",
              throwable.getMessage());
          return Future.succeededFuture(
              ApiResponse.<Void>error(
                  "Failed to restore all saldos: " + throwable.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<Void>> deleteAllPermanentSaldos() {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("SaldoService.deleteAllPermanent");

    logger.info("Attempting to permanently delete all trashed saldos");

    return repo.deleteAllPermanent()
        .compose(v -> {
          logger.info("All trashed saldos permanently deleted");
          tracingMetrics.completeSpanSuccess(
              tracingContext,
              "deleteAllPermanent",
              "All saldos permanently deleted");
          return Future.succeededFuture(
              ApiResponse.<Void>success("All saldos permanently deleted"));
        })
        .recover(throwable -> {
          logger.error("Failed to permanently delete all saldos", throwable);
          tracingMetrics.completeSpanError(
              tracingContext,
              "deleteAllPermanent",
              throwable.getMessage());
          return Future.succeededFuture(
              ApiResponse.<Void>error(
                  "Failed to permanently delete all saldos: " + throwable.getMessage()));
        });
  }
}
