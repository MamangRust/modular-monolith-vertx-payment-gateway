package io.example.merchant.service.impl;

import java.util.Map;

import io.example.common.model.ApiResponse;
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
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import pb.merchant.MerchantCommand.CreateMerchantRequest;
import pb.merchant.MerchantCommand.UpdateMerchantRequest;
import pb.merchant.MerchantCommand.UpdateMerchantStatusRequest;
import pb.user.User.UserResponse;

public class MerchantCommandServiceImpl implements MerchantCommandService {
  private final MerchantCommandRepository repo;
  private final MerchantQueryRepository repoQuery;
  private final UserClientRepository userClientRepo;
  private final RedisService redisService;
  private final KafkaService kafkaService;
  private final TracingMetrics tracingMetrics;

  public MerchantCommandServiceImpl(
      MerchantCommandRepository repo,
      MerchantQueryRepository repoQuery,
      UserClientRepository userClientRepo,
      RedisService redisService,
      KafkaService kafkaService,
      TracingMetrics tracingMetrics) {
    this.repo = repo;
    this.repoQuery = repoQuery;
    this.userClientRepo = userClientRepo;
    this.redisService = redisService;
    this.kafkaService = kafkaService;
    this.tracingMetrics = tracingMetrics;
  }

  @Override
  public Future<ApiResponse<MerchantResponse>> createMerchant(CreateMerchantRequest request) {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("MerchantCommandService.createMerchant");
    return userClientRepo.getUserById(request.getUserId())
        .compose(userObj -> {
          UserResponse user = (UserResponse) userObj;
          return repo.createMerchant(request)
              .compose(merchant -> {
                // Send Email via Kafka
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

                return kafkaService.sendMessage("email-service-topic-merchant-created",
                    String.valueOf(merchant.getId()), emailPayload)
                    .map(v -> ApiResponse.success("Merchant created successfully", MerchantResponse.from(merchant)))
                    .recover(err -> {
                      tracingMetrics.completeSpanSuccess(ctx, "createMerchant", "Success (email failed)");
                      return Future.succeededFuture(ApiResponse.success("Merchant created successfully", MerchantResponse.from(merchant)));
                    });
              });
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "createMerchant", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "createMerchant", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<MerchantResponse>> updateMerchant(UpdateMerchantRequest request) {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("MerchantCommandService.updateMerchant");
    return repo.updateMerchant(request)
        .compose(merchant -> {
          if (merchant == null)
            return Future.failedFuture("Merchant not found");
          return redisService.delete("merchant:" + merchant.getId())
              .map(v -> ApiResponse.success("Merchant updated successfully", MerchantResponse.from(merchant)));
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "updateMerchant", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "updateMerchant", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<MerchantResponse>> updateMerchantStatus(UpdateMerchantStatusRequest request) {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("MerchantCommandService.updateMerchantStatus");
    return repoQuery.findByMerchantId(request.getMerchantId())
        .compose(merchant -> {
          if (merchant == null)
            return Future.failedFuture("Merchant not found");
          return userClientRepo.getUserById(merchant.getUserId())
              .compose(userObj -> {
                UserResponse user = (UserResponse) userObj;
                return repo.updateMerchantStatus(request)
                    .compose(updated -> {
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
                          return Future.succeededFuture(updated);
                        }
                      }

                      String htmlBody = EmailTemplate.generateHtml(Map.of(
                          "Title", subject,
                          "Message", message,
                          "Button", "Go to Portal",
                          "Link", link));

                      JsonObject emailPayload = new JsonObject()
                          .put("email", user.getEmail())
                          .put("subject", subject)
                          .put("body", htmlBody);

                      return kafkaService.sendMessage("email-service-topic-merchant-update-status",
                          String.valueOf(request.getMerchantId()), emailPayload)
                          .compose(v -> redisService.delete("merchant:" + updated.getId()))
                          .map(v -> updated)
                          .recover(err -> {
                            return redisService.delete("merchant:" + updated.getId()).map(v -> updated);
                          });
                    });
              })
              .map(updated -> ApiResponse.success("Merchant status updated successfully",
                  MerchantResponse.from(updated)));
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "updateMerchantStatus", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "updateMerchantStatus", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<MerchantResponseDeleteAt>> trashedMerchant(int merchantId) {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("MerchantCommandService.trashedMerchant");
    return repo.trashedMerchant(merchantId)
        .compose(merchant -> {
          if (merchant == null)
            return Future.failedFuture("Merchant not found");
          return redisService.delete("merchant:" + merchantId)
              .map(v -> ApiResponse.success("Merchant trashed successfully", MerchantResponseDeleteAt.from(merchant)));
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "trashedMerchant", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "trashedMerchant", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<MerchantResponseDeleteAt>> restoreMerchant(int merchantId) {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("MerchantCommandService.restoreMerchant");
    return repo.restoreMerchant(merchantId)
        .map(merchant -> ApiResponse.success("Merchant restored successfully", MerchantResponseDeleteAt.from(merchant)))
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "restoreMerchant", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "restoreMerchant", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<Boolean>> deleteMerchantPermanent(int merchantId) {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("MerchantCommandService.deleteMerchantPermanent");
    return repo.deleteMerchantPermanent(merchantId)
        .compose(success -> redisService.delete("merchant:" + merchantId)
            .map(v -> ApiResponse.success("Merchant deleted permanently", success)))
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "deleteMerchantPermanent", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "deleteMerchantPermanent", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<Boolean>> restoreAllMerchant() {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("MerchantCommandService.restoreAllMerchant");
    return repo.restoreAllMerchants()
        .map(success -> ApiResponse.success("All merchants restored successfully", success))
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "restoreAllMerchant", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "restoreAllMerchant", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<Boolean>> deleteAllMerchantPermanent() {
    TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("MerchantCommandService.deleteAllMerchantPermanent");
    return repo.deleteAllMerchantsPermanent()
        .map(success -> ApiResponse.success("All merchants deleted permanently", success))
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "deleteAllMerchantPermanent", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "deleteAllMerchantPermanent", e.getMessage()));
  }
}
