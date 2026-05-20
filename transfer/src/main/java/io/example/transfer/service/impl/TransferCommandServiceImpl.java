package io.example.transfer.service.impl;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.model.ApiResponse;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.common.utils.EmailTemplate;
import io.example.transfer.model.Transfer;
import io.example.transfer.model.TransferResponse;
import io.example.transfer.model.TransferResponseDeleteAt;
import io.example.transfer.repository.CardClientRepository;
import io.example.transfer.repository.SaldoClientRepository;
import io.example.transfer.repository.TransferCommandRepository;
import io.example.transfer.service.TransferCommandService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import pb.transfer.TransferCommand.CreateTransferRequest;
import pb.transfer.TransferCommand.UpdateTransferRequest;

public class TransferCommandServiceImpl implements TransferCommandService {
  private static final Logger logger = LoggerFactory.getLogger(TransferCommandServiceImpl.class);

  private final TransferCommandRepository repo;
  private final CardClientRepository cardClientRepo;
  private final SaldoClientRepository saldoClientRepo;
  private final RedisService redisService;
  private final TracingMetrics tracingMetrics;
  private final io.example.common.service.KafkaService kafkaService;

  private static final String CACHE_PREFIX = "transfer:";

  public TransferCommandServiceImpl(
      TransferCommandRepository repo,
      CardClientRepository cardClientRepo,
      SaldoClientRepository saldoClientRepo,
      RedisService redisService,
      TracingMetrics tracingMetrics,
      io.example.common.service.KafkaService kafkaService) {
    this.repo = repo;
    this.cardClientRepo = cardClientRepo;
    this.saldoClientRepo = saldoClientRepo;
    this.redisService = redisService;
    this.tracingMetrics = tracingMetrics;
    this.kafkaService = kafkaService;
  }

  @Override
  public Future<ApiResponse<TransferResponse>> createTransfer(CreateTransferRequest req) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TransferService.createTransfer",
        Attributes.builder()
            .put("transfer.from", Objects.requireNonNull(req.getTransferFrom()))
            .put("transfer.to", Objects.requireNonNull(req.getTransferTo()))
            .put("transfer.amount", req.getTransferAmount())
            .build());

    logger.info("Creating transfer from {} to {} amount {}", req.getTransferFrom(), req.getTransferTo(),
        req.getTransferAmount());

    return cardClientRepo.findUserCardByCardNumber(req.getTransferFrom())
        .compose(senderCard -> cardClientRepo.getCardByCardNumber(req.getTransferTo())
            .compose(receiverCard -> saldoClientRepo.getSaldoByCardNumber(req.getTransferFrom()))
            .compose(senderSaldo -> {
              if (senderSaldo.getData().getTotalBalance() < req.getTransferAmount()) {
                return Future.<Transfer>failedFuture(new RuntimeException("Insufficient balance"));
              }
              return repo.createTransfer(req.getTransferFrom(), req.getTransferTo(), req.getTransferAmount());
            })
            .compose(transfer -> saldoClientRepo.getSaldoByCardNumber(req.getTransferFrom())
                .compose(sSaldo -> saldoClientRepo.updateSaldoBalance(req.getTransferFrom(),
                    (int) (sSaldo.getData().getTotalBalance() - req.getTransferAmount())))
                .compose(v -> saldoClientRepo.getSaldoByCardNumber(req.getTransferTo()))
                .compose(rSaldo -> saldoClientRepo.updateSaldoBalance(req.getTransferTo(),
                    (int) (rSaldo.getData().getTotalBalance() + req.getTransferAmount())))
                .compose(v -> repo.updateTransferStatus(transfer.getId(), "success"))
                .compose(updatedTransfer -> sendTransferEmailNotification(senderCard.getEmail(), (long) req.getTransferAmount(), updatedTransfer.getId())
                    .map(v -> {
                      tracingMetrics.completeSpanSuccess(tracingContext, "create", "Transfer created successfully");
                      return ApiResponse.success("Transfer created successfully", TransferResponse.from(updatedTransfer));
                    })
                    .recover(err -> {
                      tracingMetrics.completeSpanSuccess(tracingContext, "create", "Transfer created successfully (email failed)");
                      return Future.succeededFuture(ApiResponse.success("Transfer created successfully", TransferResponse.from(updatedTransfer)));
                    }))))
        .recover(err -> {
          logger.error("Failed to create transfer", err);
          tracingMetrics.completeSpanError(tracingContext, "create", err.getMessage());
          return Future.succeededFuture(ApiResponse.error("Failed to create transfer: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<TransferResponse>> updateTransfer(UpdateTransferRequest req) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TransferService.updateTransfer",
        Attributes.builder().put("transfer.id", req.getTransferId()).build());

    return cardClientRepo.findUserCardByCardNumber(req.getTransferFrom())
        .compose(senderCard -> repo.updateTransfer(req.getTransferId(), req.getTransferFrom(), req.getTransferTo(),
            req.getTransferAmount())
            .compose(transfer -> sendTransferEmailNotification(senderCard.getEmail(), (long) req.getTransferAmount(), transfer.getId())
                .map(v -> {
                  redisService.delete(CACHE_PREFIX + req.getTransferId());
                  tracingMetrics.completeSpanSuccess(tracingContext, "update", "Transfer updated successfully");
                  return ApiResponse.success("Transfer updated successfully", TransferResponse.from(transfer));
                })
                .recover(err -> {
                  redisService.delete(CACHE_PREFIX + req.getTransferId());
                  tracingMetrics.completeSpanSuccess(tracingContext, "update", "Transfer updated successfully (email failed)");
                  return Future.succeededFuture(ApiResponse.success("Transfer updated successfully", TransferResponse.from(transfer)));
                })))
        .recover(err -> {
          tracingMetrics.completeSpanError(tracingContext, "update", err.getMessage());
          return Future.succeededFuture(ApiResponse.error("Failed to update transfer: " + err.getMessage()));
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
        .onFailure(err -> logger.error("Failed to send transfer email notification via Kafka for transfer {}",
            transferId, err));
  }

  @Override
  public Future<ApiResponse<TransferResponseDeleteAt>> trashTransfer(int transferId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TransferService.trashTransfer",
        Attributes.builder().put("transfer.id", transferId).build());

    return repo.trashTransfer(transferId)
        .map(transfer -> {
          redisService.delete(CACHE_PREFIX + transferId);
          tracingMetrics.completeSpanSuccess(tracingContext, "trash", "Transfer trashed successfully");
          return ApiResponse.success("Transfer trashed successfully", TransferResponseDeleteAt.from(transfer));
        })
        .recover(err -> {
          tracingMetrics.completeSpanError(tracingContext, "trash", err.getMessage());
          return Future.succeededFuture(ApiResponse.error("Failed to trash transfer: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<TransferResponseDeleteAt>> restoreTransfer(int transferId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TransferService.restoreTransfer",
        Attributes.builder().put("transfer.id", transferId).build());

    return repo.restoreTransfer(transferId)
        .map(transfer -> {
          redisService.delete(CACHE_PREFIX + transferId);
          tracingMetrics.completeSpanSuccess(tracingContext, "restore", "Transfer restored successfully");
          return ApiResponse.success("Transfer restored successfully", TransferResponseDeleteAt.from(transfer));
        })
        .recover(err -> {
          tracingMetrics.completeSpanError(tracingContext, "restore", err.getMessage());
          return Future.succeededFuture(ApiResponse.error("Failed to restore transfer: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<Boolean>> deleteTransferPermanently(int transferId) {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
        "TransferService.deleteTransferPermanently",
        Attributes.builder().put("transfer.id", transferId).build());

    return repo.deleteTransferPermanently(transferId)
        .map(v -> {
          redisService.delete(CACHE_PREFIX + transferId);
          tracingMetrics.completeSpanSuccess(tracingContext, "delete_permanent", "Transfer deleted permanently");
          return ApiResponse.success("Transfer deleted permanently", true);
        })
        .recover(err -> {
          tracingMetrics.completeSpanError(tracingContext, "delete_permanent", err.getMessage());
          return Future.succeededFuture(ApiResponse.error("Failed to delete transfer: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<Boolean>> restoreAllTransfers() {
    TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("TransferService.restoreAllTransfers");

    return repo.restoreAllTransfers()
        .map(v -> {
          tracingMetrics.completeSpanSuccess(tracingContext, "restore_all", "All transfers restored");
          return ApiResponse.success("All transfers restored", true);
        })
        .recover(err -> {
          tracingMetrics.completeSpanError(tracingContext, "restore_all", err.getMessage());
          return Future.succeededFuture(ApiResponse.error("Failed to restore all transfers: " + err.getMessage()));
        });
  }

  @Override
  public Future<ApiResponse<Boolean>> deleteAllPermanentTransfers() {
    TracingMetrics.TracingContext tracingContext = tracingMetrics
        .startSpan("TransferService.deleteAllPermanentTransfers");

    return repo.deleteAllPermanentTransfers()
        .map(v -> {
          tracingMetrics.completeSpanSuccess(tracingContext, "delete_all_permanent", "All trashed transfers deleted");
          return ApiResponse.success("All trashed transfers deleted", true);
        })
        .recover(err -> {
          tracingMetrics.completeSpanError(tracingContext, "delete_all_permanent", err.getMessage());
          return Future
              .succeededFuture(ApiResponse.error("Failed to delete all trashed transfers: " + err.getMessage()));
        });
  }
}
