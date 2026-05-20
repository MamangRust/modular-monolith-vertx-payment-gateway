package io.example.merchant.service.impl;

import java.util.Map;
import io.example.common.model.ApiResponse;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.example.common.utils.EmailTemplate;
import io.example.merchant.model.MerchantDocumentResponse;
import io.example.merchant.model.MerchantDocumentResponseDeleteAt;
import io.example.merchant.repository.MerchantDocumentCommandRepository;
import io.example.merchant.repository.MerchantQueryRepository;
import io.example.merchant.repository.UserClientRepository;
import io.example.merchant.service.MerchantDocumentCommandService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import pb.merchant_document.MerchantDocumentCommand.CreateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest;
import pb.user.User.UserResponse;

public class MerchantDocumentCommandServiceImpl implements MerchantDocumentCommandService {
  private final MerchantDocumentCommandRepository repo;
  private final MerchantQueryRepository merchantQueryRepo;
  private final UserClientRepository userClientRepo;
  private final RedisService redisService;
  private final KafkaService kafkaService;
  private final TracingMetrics tracingMetrics;

  public MerchantDocumentCommandServiceImpl(
      MerchantDocumentCommandRepository repo,
      MerchantQueryRepository merchantQueryRepo,
      UserClientRepository userClientRepo,
      RedisService redisService,
      KafkaService kafkaService,
      TracingMetrics tracingMetrics) {
    this.repo = repo;
    this.merchantQueryRepo = merchantQueryRepo;
    this.userClientRepo = userClientRepo;
    this.redisService = redisService;
    this.kafkaService = kafkaService;
    this.tracingMetrics = tracingMetrics;
  }

  @Override
  public Future<ApiResponse<MerchantDocumentResponse>> createMerchantDocument(CreateMerchantDocumentRequest request) {
    TracingMetrics.TracingContext ctx = tracingMetrics
        .startSpan("MerchantDocumentCommandService.createMerchantDocument");
    return merchantQueryRepo.findByMerchantId(request.getMerchantId())
        .compose(merchant -> {
          if (merchant == null)
            return Future.failedFuture("Merchant not found");
          return userClientRepo.getUserById(merchant.getUserId())
              .compose(userObj -> {
                UserResponse user = (UserResponse) userObj;
                return repo.createMerchantDocument(request)
                    .compose(doc -> {
                      String htmlBody = EmailTemplate.generateHtml(Map.of(
                          "Title", "Welcome to SanEdge Merchant Portal",
                          "Message",
                          "Thank you for registering your merchant account. Your account is currently <b>inactive</b> and under initial review. To proceed, please upload all required documents for verification. Once your documents are submitted, our team will review them and activate your account accordingly.",
                          "Button", "Upload Documents",
                          "Link", String.format("https://sanedge.example.com/merchant/%d/documents", user.getId())));

                      JsonObject emailPayload = new JsonObject()
                          .put("email", user.getEmail())
                          .put("subject", "Merchant Verification Pending - Action Required")
                          .put("body", htmlBody);

                      return kafkaService.sendMessage("email-service-topic-merchant-document-created",
                          String.valueOf(doc.getId()), emailPayload)
                          .map(v -> doc)
                          .recover(err -> Future.succeededFuture(doc));
                    });
              });
        })
        .map(doc -> ApiResponse.success("Merchant document created successfully", MerchantDocumentResponse.from(doc)))
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "createMerchantDocument", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "createMerchantDocument", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<MerchantDocumentResponse>> updateMerchantDocument(UpdateMerchantDocumentRequest request) {
    TracingMetrics.TracingContext ctx = tracingMetrics
        .startSpan("MerchantDocumentCommandService.updateMerchantDocument");
    return repo.updateMerchantDocument(request)
        .compose(doc -> {
          if (doc == null)
            return Future.failedFuture("Document not found");
          return redisService.delete("merchant_document:" + doc.getId())
              .map(v -> ApiResponse.success("Merchant document updated successfully",
                  MerchantDocumentResponse.from(doc)));
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "updateMerchantDocument", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "updateMerchantDocument", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<MerchantDocumentResponse>> updateMerchantDocumentStatus(
      UpdateMerchantDocumentStatusRequest request) {
    TracingMetrics.TracingContext ctx = tracingMetrics
        .startSpan("MerchantDocumentCommandService.updateMerchantDocumentStatus");
    return merchantQueryRepo.findByMerchantId(request.getMerchantId())
        .compose(merchant -> {
          if (merchant == null)
            return Future.failedFuture("Merchant not found");
          return userClientRepo.getUserById(merchant.getUserId())
              .compose(userObj -> {
                UserResponse user = (UserResponse) userObj;
                return repo.updateMerchantDocumentStatus(request)
                    .compose(updated -> {
                      String status = request.getStatus();
                      String note = request.getNote();
                      String subject = "";
                      String message = "";
                      String buttonLabel = "";
                      String link = String.format("https://sanedge.example.com/merchant/%d/documents",
                          request.getMerchantId());

                      switch (status) {
                        case "pending" -> {
                          subject = "Merchant Document Status: Pending Review";
                          message = "Your merchant documents have been submitted and are currently pending review.";
                          buttonLabel = "View Documents";
                        }
                        case "approved" -> {
                          subject = "Merchant Document Status: Approved";
                          message = "Congratulations! Your merchant documents have been approved. Your account is now active and fully functional.";
                          buttonLabel = "Go to Dashboard";
                          link = String.format("https://sanedge.example.com/merchant/%d/dashboard",
                              request.getMerchantId());
                        }
                        case "rejected" -> {
                          subject = "Merchant Document Status: Rejected";
                          message = "Unfortunately, your merchant documents were rejected. Please review the feedback below and re-upload the necessary documents.";
                          buttonLabel = "Re-upload Documents";
                        }
                        default -> {
                          return Future.succeededFuture(updated);
                        }
                      }

                      if (note != null && !note.isEmpty()) {
                        message += String.format("<br><br><b>Reviewer Note:</b><br><i>%s</i>", note);
                      }

                      String htmlBody = EmailTemplate.generateHtml(Map.of(
                          "Title", subject,
                          "Message", message,
                          "Button", buttonLabel,
                          "Link", link));

                      JsonObject emailPayload = new JsonObject()
                          .put("email", user.getEmail())
                          .put("subject", subject)
                          .put("body", htmlBody);

                      return kafkaService.sendMessage("email-service-topic-merchant-document-update-status",
                          String.valueOf(request.getMerchantId()), emailPayload)
                          .compose(v -> redisService.delete("merchant_document:" + updated.getId()))
                          .map(v -> updated)
                          .recover(err -> redisService.delete("merchant_document:" + updated.getId()).map(v -> updated));
                    });
              });
        })
        .map(updated -> ApiResponse.success("Merchant document status updated successfully",
            MerchantDocumentResponse.from(updated)))
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "updateMerchantDocumentStatus", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "updateMerchantDocumentStatus", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<MerchantDocumentResponseDeleteAt>> trashedMerchantDocument(int documentId) {
    TracingMetrics.TracingContext ctx = tracingMetrics
        .startSpan("MerchantDocumentCommandService.trashedMerchantDocument");
    return repo.trashedMerchantDocument(documentId)
        .compose(doc -> {
          if (doc == null)
            return Future.failedFuture("Document not found");
          return redisService.delete("merchant_document:" + documentId)
              .map(v -> ApiResponse.success("Merchant document trashed successfully",
                  MerchantDocumentResponseDeleteAt.from(doc)));
        })
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "trashedMerchantDocument", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "trashedMerchantDocument", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<MerchantDocumentResponseDeleteAt>> restoreMerchantDocument(int documentId) {
    TracingMetrics.TracingContext ctx = tracingMetrics
        .startSpan("MerchantDocumentCommandService.restoreMerchantDocument");
    return repo.restoreMerchantDocument(documentId)
        .map(doc -> ApiResponse.success("Merchant document restored successfully",
            MerchantDocumentResponseDeleteAt.from(doc)))
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "restoreMerchantDocument", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "restoreMerchantDocument", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<Boolean>> deleteMerchantDocumentPermanent(int documentId) {
    TracingMetrics.TracingContext ctx = tracingMetrics
        .startSpan("MerchantDocumentCommandService.deleteMerchantDocumentPermanent");
    return repo.deleteMerchantDocumentPermanent(documentId)
        .compose(success -> redisService.delete("merchant_document:" + documentId)
            .map(v -> ApiResponse.success("Merchant document deleted permanently", success)))
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "deleteMerchantDocumentPermanent", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "deleteMerchantDocumentPermanent", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<Boolean>> restoreAllMerchantDocument() {
    TracingMetrics.TracingContext ctx = tracingMetrics
        .startSpan("MerchantDocumentCommandService.restoreAllMerchantDocument");
    return repo.restoreAllMerchantDocuments()
        .map(success -> ApiResponse.success("All merchant documents restored successfully", success))
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "restoreAllMerchantDocument", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "restoreAllMerchantDocument", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<Boolean>> deleteAllMerchantDocumentPermanent() {
    TracingMetrics.TracingContext ctx = tracingMetrics
        .startSpan("MerchantDocumentCommandService.deleteAllMerchantDocumentPermanent");
    return repo.deleteAllMerchantDocumentsPermanent()
        .map(success -> ApiResponse.success("All merchant documents deleted permanently", success))
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "deleteAllMerchantDocumentPermanent", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "deleteAllMerchantDocumentPermanent", e.getMessage()));
  }
}
