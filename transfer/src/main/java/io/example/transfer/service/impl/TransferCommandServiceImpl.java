package io.example.transfer.service.impl;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.ConflictException;
import io.example.common.exception.grpc.InsufficientBalanceException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.common.utils.EmailTemplate;
import io.example.transfer.model.Transfer;
import io.example.transfer.model.TransferResponse;
import io.example.transfer.model.TransferResponseDeleteAt;
import io.example.transfer.repository.CardClientRepository;
import io.example.transfer.repository.SaldoClientRepository;
import io.example.transfer.repository.TransferCommandRepository;
import io.example.transfer.repository.TransferQueryRepository;
import io.example.transfer.service.TransferCommandService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;
import pb.saldo.Saldo.ApiResponseSaldo;
import pb.transfer.TransferCommand.CreateTransferRequest;
import pb.transfer.TransferCommand.UpdateTransferRequest;

@RequiredArgsConstructor
public class TransferCommandServiceImpl implements TransferCommandService {
  private static final Logger logger = LoggerFactory.getLogger(TransferCommandServiceImpl.class);
  private static final String CACHE_PREFIX = "transfer:";

  private final TransferCommandRepository repo;
  private final TransferQueryRepository queryRepository;
  private final CardClientRepository cardClientRepo;
  private final SaldoClientRepository saldoClientRepo;
  private final RedisService redisService;
  private final TracingMetrics tracingMetrics;
  private final io.example.common.service.KafkaService kafkaService;

  private Future<Void> invalidateCache(Integer transferId) {
    return redisService.delete(CACHE_PREFIX + transferId)
        .compose(v -> redisService.delete(CACHE_PREFIX + "list:*"))
        .<Void>mapEmpty();
  }

  private Future<Void> invalidateListCache() {
    return redisService.delete(CACHE_PREFIX + "list:*").<Void>mapEmpty();
  }

  @Override
  public Future<TransferResponse> createTransfer(CreateTransferRequest req) {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan(
        "TransferService.createTransfer",
        Attributes.builder()
            .put("transfer.from", Objects.requireNonNull(req.getTransferFrom()))
            .put("transfer.to", Objects.requireNonNull(req.getTransferTo()))
            .put("transfer.amount", req.getTransferAmount())
            .build());

    logger.info("Creating transfer from {} to {} amount {}", req.getTransferFrom(), req.getTransferTo(),
        req.getTransferAmount());

    String idempotencyKey = req.getIdempotencyKey();
    Future<TransferContext> idempotencyCheck = idempotencyKey != null && !idempotencyKey.isBlank()
        ? repo.findByIdempotencyKey(idempotencyKey)
            .compose(existing -> existing == null
                ? Future.succeededFuture(new TransferContext(null, false))
                : replayOrReject(existing, req).map(replayed -> new TransferContext(replayed, true)))
        : Future.succeededFuture(new TransferContext(null, false));

    return idempotencyCheck
        .<TransferContext>compose(existingContext -> {
          if (existingContext.replayed) {
            Transfer existing = existingContext.transfer;
            logger.info("Replaying existing transfer {} for idempotency key {}", existing.getId(), idempotencyKey);
            return Future.succeededFuture(existingContext);
          }

          return cardClientRepo.findUserCardByCardNumber(req.getTransferFrom())
              .<TransferContext>compose(senderCard -> cardClientRepo.getCardByCardNumber(req.getTransferTo())
                  // Receiver card existence check (the card object itself is not
                  // needed beyond validation).
                  .<ApiResponseSaldo>compose(v -> saldoClientRepo.getSaldoByCardNumber(req.getTransferFrom()))
                  .<TransferContext>compose(senderSaldo -> {
                    int currentBalance = senderSaldo.getData().getTotalBalance();
                    if (currentBalance < req.getTransferAmount()) {
                      return Future.<TransferContext>failedFuture(
                          new InsufficientBalanceException(currentBalance, req.getTransferAmount()));
                    }
                    return repo.createTransfer(req.getTransferFrom(), req.getTransferTo(),
                        req.getTransferAmount(), idempotencyKey)
                        .<TransferContext>compose(created -> {
                          if (created != null) {
                            return Future.succeededFuture(new TransferContext(created, false));
                          }
                          if (idempotencyKey == null || idempotencyKey.isBlank()) {
                            return Future.<TransferContext>failedFuture(
                                new IllegalStateException("Transfer insert returned no row"));
                          }
                          // A concurrent request won the unique key. Re-read
                          // and replay only a completed operation.
                          return repo.findByIdempotencyKey(idempotencyKey)
                              .compose(raced -> replayOrReject(raced, req)
                                  .map(replayed -> new TransferContext(replayed, true)));
                        });
                  })
                  .<TransferContext>compose(transferContext -> {
                    if (transferContext.replayed) {
                      return Future.succeededFuture(transferContext);
                    }
                    Transfer transfer = transferContext.transfer;
                    // Leg 1: atomic guarded debit of the sender (no read-modify-write).
                    // If this fails (e.g. race made the balance insufficient), the
                    // balance was NOT changed, so only mark the transfer failed.
                    return saldoClientRepo.updateSaldoDelta(req.getTransferFrom(), -req.getTransferAmount())
                        .recover(err -> failAfterStatus(transfer, "failed", err))
                        .compose(v -> {
                          // Leg 2: atomic credit of the receiver. On failure the
                          // sender debit is compensated so no balance is left
                          // changed (SUPERPLANNING B.7 #3).
                          return saldoClientRepo.updateSaldoDelta(req.getTransferTo(), req.getTransferAmount())
                              // Keep receiver-credit failure separate: no receiver
                              // balance was changed, so only reverse the sender leg.
                              .recover(err -> compensateSender(req, transfer, err))
                              // From this point the receiver was credited. Any
                              // finalization failure must reverse both legs.
                              .compose(w -> repo.updateTransferStatus(transfer.getId(), "success")
                                  .compose(updatedTransfer -> {
                                    if (updatedTransfer == null) {
                                      return Future.<TransferContext>failedFuture(
                                          new IllegalStateException("Transfer status update returned no row"));
                                    }
                                    return sendTransferEmailNotification(senderCard.getEmail(),
                                        (long) req.getTransferAmount(), updatedTransfer.getId())
                                        .map(v2 -> new TransferContext(updatedTransfer, false));
                                  })
                                  .recover(err -> markTransferFinalizationAmbiguous(transfer, err)));
                        });
                  }));
        })
        .compose(transferContext -> invalidateListCache().map(v -> transferContext))
        .map(transferContext -> TransferResponse.from(transferContext.transfer))
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "create", "Transfer created successfully"))
        .onFailure(err -> {
          logger.error("Failed to create transfer", err);
          tracingMetrics.completeSpanError(ctx, "create", err.getMessage());
        });
  }

  private static class TransferContext {
    final Transfer transfer;
    final boolean replayed;

    TransferContext(Transfer transfer, boolean replayed) {
      this.transfer = transfer;
      this.replayed = replayed;
    }
  }

  private Future<Transfer> replayOrReject(Transfer existing, CreateTransferRequest req) {
    if (!sameTransferRequest(existing, req)) {
      return Future.failedFuture(new ConflictException(
          "Idempotency key was already used for a different transfer"));
    }
    if (!"success".equalsIgnoreCase(existing.getStatus())) {
      String status = existing.getStatus() == null ? "unknown" : existing.getStatus();
      return Future.failedFuture(new ConflictException(
          "Transfer with this idempotency key is not replayable (status: " + status + ")"));
    }
    return Future.succeededFuture(existing);
  }

  private boolean sameTransferRequest(Transfer existing, CreateTransferRequest req) {
    return existing != null
        && Objects.equals(existing.getTransferFrom(), req.getTransferFrom())
        && Objects.equals(existing.getTransferTo(), req.getTransferTo())
        && Objects.equals(existing.getTransferAmount(), (long) req.getTransferAmount());
  }

  /**
   * Reverses the sender debit after a failed receiver credit, then marks the
   * transfer failed and propagates the original error. The balance is restored
   * even when the compensation itself fails (original error is kept).
   */
  private <T> Future<T> markTransferFinalizationAmbiguous(Transfer transfer, Throwable statusError) {
    return persistTransferStatus(transfer, "compensation_required")
        .compose(updated -> {
          IllegalStateException failure = new IllegalStateException(
              "Transfer success status is ambiguous for " + transfer.getId());
          failure.addSuppressed(statusError);
          return Future.<T>failedFuture(failure);
        })
        .recover(markerError -> {
          markerError.addSuppressed(statusError);
          return Future.failedFuture(markerError);
        });
  }

  private <T> Future<T> compensateSender(CreateTransferRequest req, Transfer transfer, Throwable err) {
    return saldoClientRepo.updateSaldoDelta(req.getTransferFrom(), req.getTransferAmount())
        .<Void>mapEmpty()
        .recover(compensationErr -> markTransferCompensationRequired(transfer, err, compensationErr))
        .compose(v -> failAfterStatus(transfer, "failed", err));
  }

  private <T> Future<T> failAfterStatus(Transfer transfer, String status, Throwable err) {
    return persistTransferStatus(transfer, status)
        .recover(statusError -> markTransferCompensationRequired(transfer, err, statusError))
        .compose(ignored -> Future.<T>failedFuture(err));
  }

  private Future<Transfer> persistTransferStatus(Transfer transfer, String status) {
    return repo.updateTransferStatus(transfer.getId(), status)
        .compose(updated -> {
          if (updated == null) {
            return Future.<Transfer>failedFuture(new IllegalStateException(
                "Could not persist transfer " + transfer.getId() + " status '" + status + "'"));
          }
          return Future.succeededFuture(updated);
        });
  }

  private <T> Future<T> markTransferCompensationRequired(Transfer transfer, Throwable originalError,
      Throwable compensationError) {
    return persistTransferStatus(transfer, "compensation_required")
        .compose(updated -> {
          IllegalStateException failure = new IllegalStateException(
              "Transfer compensation could not be completed for " + transfer.getId());
          failure.addSuppressed(originalError);
          failure.addSuppressed(compensationError);
          return Future.<T>failedFuture(failure);
        })
        .recover(statusError -> {
          statusError.addSuppressed(originalError);
          statusError.addSuppressed(compensationError);
          return Future.failedFuture(statusError);
        });
  }

  @Override
  public Future<TransferResponse> updateTransfer(UpdateTransferRequest req) {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan(
        "TransferService.updateTransfer",
        Attributes.builder().put("transfer.id", req.getTransferId()).build());

    return cardClientRepo.findUserCardByCardNumber(req.getTransferFrom())
        .compose(senderCard -> repo.updateTransfer(req.getTransferId(), req.getTransferFrom(), req.getTransferTo(),
            req.getTransferAmount())
            .compose(transfer -> {
              if (transfer == null) {
                return Future.<Transfer>failedFuture(new NotFoundException("Transfer not found"));
              }
              return sendTransferEmailNotification(senderCard.getEmail(), (long) req.getTransferAmount(),
                  transfer.getId()).map(v -> transfer);
            })
            .compose(transfer -> invalidateCache(req.getTransferId()).map(v -> transfer)))
        .map(TransferResponse::from)
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "update", "Transfer updated successfully"))
        .onFailure(err -> {
          logger.error("Failed to update transfer", err);
          tracingMetrics.completeSpanError(ctx, "update", err.getMessage());
        });
  }

  @Override
  public Future<TransferResponseDeleteAt> trashTransfer(Integer transferId) {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan(
        "TransferService.trashTransfer",
        Attributes.builder().put("transfer.id", (long) transferId).build());

    return repo.trashTransfer(transferId)
        .compose(transfer -> {
          if (transfer == null) {
            return Future.<io.example.transfer.model.Transfer>failedFuture(new NotFoundException("Transfer not found"));
          }
          return invalidateCache(transferId).map(v -> transfer);
        })
        .map(TransferResponseDeleteAt::from)
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "trash", "Transfer trashed successfully"))
        .onFailure(err -> {
          logger.error("Failed to trash transfer: {}", transferId, err);
          tracingMetrics.completeSpanError(ctx, "trash", err.getMessage());
        });
  }

  @Override
  public Future<TransferResponseDeleteAt> restoreTransfer(Integer transferId) {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan(
        "TransferService.restoreTransfer",
        Attributes.builder().put("transfer.id", transferId).build());

    return queryRepository.findByTrashedId(transferId)
        .compose(trashed -> {
          if (trashed == null)
            return Future.failedFuture(new BadRequestException("Transfer not found or must be trashed first"));
          return repo.restoreTransfer(transferId);
        })
        .compose(transfer -> {
          if (transfer == null) {
            return Future.<io.example.transfer.model.Transfer>failedFuture(new NotFoundException("Transfer not found"));
          }
          return invalidateCache(transferId).<io.example.transfer.model.Transfer>map(v -> transfer);
        })
        .map(TransferResponseDeleteAt::from)
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "restore", "Transfer restored successfully"))
        .onFailure(err -> {
          logger.error("Failed to restore transfer: {}", transferId, err);
          tracingMetrics.completeSpanError(ctx, "restore", err.getMessage());
        });
  }

  @Override
  public Future<Void> deleteTransferPermanently(Integer transferId) {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("TransferService.deletePermanent",
        Attributes.builder().put("transfer.id", (long) transferId).build());

    return queryRepository.findByTrashedId(transferId)
        .compose(trashed -> {
          if (trashed == null) {
            return Future.<Void>failedFuture(
                new BadRequestException("Transfer not found or must be trashed first"));
          }
          return repo.deleteTransferPermanently(transferId)
              .compose(deleted -> {
                if (!deleted) {
                  return Future.<Void>failedFuture(
                      new BadRequestException("Transfer not found or must be trashed first"));
                }
                return invalidateCache(transferId);
              });
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx,
            "deletePermanent", "Transfer deleted permanently"))
        .onFailure(err -> {
          logger.error("Failed to deletePermanent transfer: {}", transferId, err);
          tracingMetrics.completeSpanError(ctx, "deletePermanent", err.getMessage());
        });
  }

  @Override
  public Future<Void> restoreAllTransfers() {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("TransferService.restoreAll");

    return repo.restoreAllTransfers()
        .compose(count -> {
          if (count == 0) {
            return Future.<Void>failedFuture(new NotFoundException("No trashed transfers found"));
          }
          return invalidateListCache();
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "restore_all", "All transfers restored"))
        .onFailure(err -> {
          logger.error("Failed to restore all transfers", err);
          tracingMetrics.completeSpanError(ctx, "restore_all", err.getMessage());
        });
  }

  @Override
  public Future<Void> deleteAllPermanentTransfers() {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("TransferService.deleteAllPermanent");

    return repo.deleteAllPermanentTransfers()
        .compose(count -> {
          if (count == 0) {
            return Future.<Void>failedFuture(new NotFoundException("No trashed transfers found"));
          }
          return invalidateListCache();
        })
        .onSuccess(
            v -> tracingMetrics.completeSpanSuccess(ctx, "deleteAllPermanent", "All transfers permanently deleted"))
        .onFailure(err -> {
          logger.error("Failed to permanently delete all transfers", err);
          tracingMetrics.completeSpanError(ctx, "deleteAllPermanent", err.getMessage());
        });
  }

  private Future<Void> sendTransferEmailNotification(String email, Long amount, Integer transferId) {
    if (kafkaService == null) {
      logger.warn("KafkaService is null, skipping email notification for transfer {}", transferId);
      return Future.succeededFuture();
    }

    String htmlBody = EmailTemplate.generateHtml(Map.of(
        "Title", "Transfer Successful",
        "Message", String.format("Your transfer of %d has been processed successfully.", amount),
        "Button", "View History",
        "Link", "https://sanedge.example.com/transfer/history"));

    JsonObject emailPayload = new JsonObject()
        .put("email", email)
        .put("subject", "Transfer Successful - SanEdge")
        .put("body", htmlBody);

    return kafkaService.sendMessage("email-service-topic-transfer-create", String.valueOf(transferId), emailPayload)
        .<Void>mapEmpty()
        .onFailure(err -> logger.error("Failed to send transfer email notification via Kafka for transfer {}",
            transferId, err))
        .recover(err -> Future.succeededFuture());
  }
}