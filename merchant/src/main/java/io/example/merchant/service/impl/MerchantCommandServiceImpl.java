package io.example.merchant.service.impl;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.example.common.utils.EmailTemplate;
import io.example.merchant.model.MerchantResponse;
import io.example.merchant.model.MerchantResponseDeleteAt;
import io.example.merchant.repository.MerchantCommandRepository;
import io.example.merchant.repository.MerchantQueryRepository;
import io.example.merchant.repository.UserClientRepository;
import io.example.merchant.service.MerchantCommandService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;
import pb.merchant.MerchantCommand.CreateMerchantRequest;
import pb.merchant.MerchantCommand.UpdateMerchantRequest;
import pb.merchant.MerchantCommand.UpdateMerchantStatusRequest;
import pb.user.User.UserResponse;

@RequiredArgsConstructor
public class MerchantCommandServiceImpl implements MerchantCommandService {
  private static final Logger logger = LoggerFactory.getLogger(MerchantCommandServiceImpl.class);

  private final MerchantCommandRepository repo;
  private final MerchantQueryRepository repoQuery;
  private final UserClientRepository userClientRepo;
  private final RedisService redisService;
  private final KafkaService kafkaService;
  private final TracingMetrics tracingMetrics;

  private Future<Void> invalidateListCache() {
    return redisService.delete("merchant:list")
        .compose(v -> redisService.delete("merchant:list:trashed")).<Void>mapEmpty();
  }

  @Override
  public Future<MerchantResponse> createMerchant(CreateMerchantRequest request) {
    var ctx = tracingMetrics.startSpan("MerchantCommandService.createMerchant");
    return userClientRepo.getUserById(request.getUserId())
        .compose(userObj -> {
          UserResponse user = (UserResponse) userObj;
          return repo.createMerchant(request)
              .compose(merchant -> {
                String htmlBody = EmailTemplate.generateHtml(Map.of(
                    "Title", "Welcome to SanEdge Merchant Portal",
                    "Message",
                    "Your merchant account has been created successfully. To continue, please upload the required documents for verification. Once completed, our team will review and activate your account.",
                    "Button", "Upload Documents",
                    "Link", String.format("https://sanedge.example.com/merchant/%d/documents", user.getId())));

                JsonObject emailPayload = new JsonObject()
                    .put("email", user.getEmail())
                    .put("subject", "Initial Verification - SanEdge")
                    .put("body", htmlBody);

                return kafkaService
                    .sendMessage("email-service-topic-merchant-created", String.valueOf(merchant.getId()), emailPayload)
                    .map(v -> merchant)
                    .recover(err -> Future.succeededFuture(merchant));
              });
        })
        .map(MerchantResponse::from)
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "createMerchant", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "createMerchant", e.getMessage()));
  }

  @Override
  public Future<MerchantResponse> updateMerchant(UpdateMerchantRequest request) {
    var ctx = tracingMetrics.startSpan("MerchantCommandService.updateMerchant");
    return repo.updateMerchant(request)
        .compose(merchant -> {
          if (merchant == null)
            return Future.failedFuture(new NotFoundException("Merchant not found"));
          return redisService.delete("merchant:" + merchant.getId()).map(v -> merchant);
        })
        .map(MerchantResponse::from)
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "updateMerchant", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "updateMerchant", e.getMessage()));
  }

  @Override
  public Future<MerchantResponse> updateMerchantStatus(UpdateMerchantStatusRequest request) {
    var ctx = tracingMetrics.startSpan("MerchantCommandService.updateMerchantStatus");
    return repoQuery.findByMerchantId(request.getMerchantId())
        .compose(merchant -> {
          if (merchant == null)
            return Future.failedFuture(new NotFoundException("Merchant not found"));
          return userClientRepo.getUserById(merchant.getUserId())
              .compose(userObj -> {
                UserResponse user = (UserResponse) userObj;
                return repo.updateMerchantStatus(request)
                    .compose(updated -> {
                      if (updated == null)
                        return Future.failedFuture(new NotFoundException("Merchant not found on update"));

                      String status = request.getStatus();
                      String subject = "";
                      String message = "";
                      String link = String.format("https://sanedge.example.com/merchant/%d/dashboard",
                          request.getMerchantId());

                      switch (status) {
                        case "active" -> {
                          subject = "Your Merchant Account is Now Active";
                          message = "Congratulations! Your merchant account has been verified and is now <b>active</b>. You can now fully access all features in the SanEdge Merchant Portal.";
                        }
                        case "inactive" -> {
                          subject = "Merchant Account Set to Inactive";
                          message = "Your merchant account status has been set to <b>inactive</b>. Please contact support if you believe this is a mistake.";
                        }
                        case "rejected" -> {
                          subject = "Merchant Account Rejected";
                          message = "We're sorry to inform you that your merchant account has been <b>rejected</b>. Please contact support or review your submissions.";
                        }
                        default -> {
                          return redisService.delete("merchant:" + updated.getId()).map(v -> updated);
                        }
                      }

                      String htmlBody = EmailTemplate.generateHtml(
                          Map.of("Title", subject, "Message", message, "Button", "Go to Portal", "Link", link));
                      JsonObject emailPayload = new JsonObject().put("email", user.getEmail()).put("subject", subject)
                          .put("body", htmlBody);

                      return kafkaService
                          .sendMessage("email-service-topic-merchant-update-status",
                              String.valueOf(request.getMerchantId()), emailPayload)
                          .compose(v -> redisService.delete("merchant:" + updated.getId())).map(v -> updated)
                          .recover(err -> redisService.delete("merchant:" + updated.getId()).map(v -> updated));
                    });
              });
        })
        .map(MerchantResponse::from)
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "updateMerchantStatus", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "updateMerchantStatus", e.getMessage()));
  }

  @Override
  public Future<MerchantResponseDeleteAt> trashedMerchant(Integer merchantId) {
    var ctx = tracingMetrics.startSpan("MerchantCommandService.trashedMerchant");
    return repo.trashedMerchant(merchantId)
        .compose(merchant -> {
          if (merchant == null)
            return Future.failedFuture(new NotFoundException("Merchant not found"));
          return redisService.delete("merchant:" + merchantId).map(v -> merchant);
        })
        .map(MerchantResponseDeleteAt::from)
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "trashedMerchant", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "trashedMerchant", e.getMessage()));
  }

  @Override
  public Future<MerchantResponseDeleteAt> restoreMerchant(Integer merchantId) {
    var ctx = tracingMetrics.startSpan("MerchantCommandService.restoreMerchant");
    return repoQuery.findByTrashedById(merchantId)
        .compose(trashed -> {
          if (trashed == null)
            return Future.failedFuture(new BadRequestException("Merchant not found or must be trashed first"));
          return repo.restoreMerchant(merchantId);
        })
        .compose(restored -> {
          if (restored == null)
            return Future.failedFuture(new NotFoundException("Merchant not found on restore"));
          return redisService.delete("merchant:" + merchantId).map(v -> restored);
        })
        .map(MerchantResponseDeleteAt::from)
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "restoreMerchant", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "restoreMerchant", e.getMessage()));
  }

  @Override
  public Future<Void> deleteMerchantPermanent(Integer merchantId) {
    var ctx = tracingMetrics.startSpan("MerchantCommandService.deleteMerchantPermanent",
        Attributes.builder().put("merchant.id", merchantId).build());

    return repoQuery.findByTrashedById(merchantId)
        .compose(trashed -> {
          if (trashed == null)
            return Future.failedFuture(new BadRequestException("Merchant not found or must be trashed first"));
          return repo.deleteMerchantPermanent(merchantId);
        })
        .compose(deleted -> {
          if (!deleted)
            return Future.failedFuture(new BadRequestException("Merchant not found or must be trashed first"));
          return redisService.delete("merchant:" + merchantId).map(v -> (Void) null);
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "deleteMerchantPermanent", "Success"))
        .onFailure(e -> {
          logger.error("Failed to deletePermanent merchant: {}", merchantId, e);
          tracingMetrics.completeSpanError(ctx, "deleteMerchantPermanent", e.getMessage());
        });
  }

  @Override
  public Future<Void> restoreAllMerchant() {
    var ctx = tracingMetrics.startSpan("MerchantCommandService.restoreAllMerchant");
    return repo.restoreAllMerchants()
        .compose(count -> {
          if (count == 0) {
            return Future.<Void>failedFuture(new NotFoundException("No trashed merchants found"));
          }

          return invalidateListCache();
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "restoreAllMerchant", "Success"))
        .onFailure(e -> {
          logger.error("Failed to restore all merchants", e);
          tracingMetrics.completeSpanError(ctx, "restoreAllMerchant", e.getMessage());
        });
  }

  @Override
  public Future<Void> deleteAllMerchantPermanent() {
    var ctx = tracingMetrics.startSpan("MerchantCommandService.deleteAllMerchantPermanent");
    return repo.deleteAllMerchantsPermanent()
        .compose(count -> {
          if (count == 0) {
            return Future.<Void>failedFuture(new NotFoundException("No trashed merchants found"));
          }
          return invalidateListCache();
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "deleteAllMerchantPermanent", "Success"))
        .onFailure(e -> {
          logger.error("Failed to permanently delete all merchants", e);
          tracingMetrics.completeSpanError(ctx, "deleteAllMerchantPermanent", e.getMessage());
        });
  }
}