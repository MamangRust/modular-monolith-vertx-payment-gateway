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
import io.example.merchant.model.MerchantDocumentResponse;
import io.example.merchant.model.MerchantDocumentResponseDeleteAt;
import io.example.merchant.repository.MerchantDocumentCommandRepository;
import io.example.merchant.repository.MerchantDocumentQueryRepository;
import io.example.merchant.repository.MerchantQueryRepository;
import io.example.merchant.repository.UserClientRepository;
import io.example.merchant.service.MerchantDocumentCommandService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;
import pb.merchant_document.MerchantDocumentCommand.CreateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest;
import pb.user.User.UserResponse;

@RequiredArgsConstructor
public class MerchantDocumentCommandServiceImpl implements MerchantDocumentCommandService {
  private static final Logger logger = LoggerFactory.getLogger(MerchantDocumentCommandServiceImpl.class);

  private final MerchantDocumentCommandRepository repo;
  private final MerchantDocumentQueryRepository queryRepository;
  private final MerchantQueryRepository merchantQueryRepo;
  private final UserClientRepository userClientRepo;
  private final RedisService redisService;
  private final KafkaService kafkaService;
  private final TracingMetrics tracingMetrics;

  private Future<Void> invalidateListCache() {
    return redisService.delete("merchant_document:list")
        .compose(v -> redisService.delete("merchant_document:list:trashed")).<Void>mapEmpty();
  }

  @Override
  public Future<MerchantDocumentResponse> createMerchantDocument(CreateMerchantDocumentRequest request) {
    var ctx = tracingMetrics.startSpan("MerchantDocumentCommandService.createMerchantDocument");
    return merchantQueryRepo.findByMerchantId(request.getMerchantId())
        .compose(merchant -> {
          if (merchant == null)
            return Future.failedFuture(new NotFoundException("Merchant not found"));
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

                      return kafkaService
                          .sendMessage("email-service-topic-merchant-document-create", String.valueOf(doc.getId()),
                              emailPayload)
                          .map(v -> doc)
                          .recover(err -> Future.succeededFuture(doc));
                    });
              });
        })
        .map(MerchantDocumentResponse::from)
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "createMerchantDocument", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "createMerchantDocument", e.getMessage()));
  }

  @Override
  public Future<MerchantDocumentResponse> updateMerchantDocument(UpdateMerchantDocumentRequest request) {
    var ctx = tracingMetrics.startSpan("MerchantDocumentCommandService.updateMerchantDocument");
    return repo.updateMerchantDocument(request)
        .compose(doc -> {
          if (doc == null)
            return Future.failedFuture(new NotFoundException("Document not found"));
          return redisService.delete("merchant_document:" + doc.getId()).map(v -> doc);
        })
        .map(MerchantDocumentResponse::from)
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "updateMerchantDocument", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "updateMerchantDocument", e.getMessage()));
  }

  @Override
  public Future<MerchantDocumentResponse> updateMerchantDocumentStatus(UpdateMerchantDocumentStatusRequest request) {
    var ctx = tracingMetrics.startSpan("MerchantDocumentCommandService.updateMerchantDocumentStatus");
    return merchantQueryRepo.findByMerchantId(request.getMerchantId())
        .compose(merchant -> {
          if (merchant == null)
            return Future.failedFuture(new NotFoundException("Merchant not found"));
          return userClientRepo.getUserById(merchant.getUserId())
              .compose(userObj -> {
                UserResponse user = (UserResponse) userObj;
                return repo.updateMerchantDocumentStatus(request)
                    .compose(updated -> {
                      if (updated == null)
                        return Future.failedFuture(new NotFoundException("Document not found on update"));

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
                          return redisService.delete("merchant_document:" + updated.getId()).map(v -> updated);
                        }
                      }

                      if (note != null && !note.isEmpty()) {
                        message += String.format("<br><br><b>Reviewer Note:</b><br><i>%s</i>", note);
                      }

                      String htmlBody = EmailTemplate.generateHtml(
                          Map.of("Title", subject, "Message", message, "Button", buttonLabel, "Link", link));
                      JsonObject emailPayload = new JsonObject().put("email", user.getEmail()).put("subject", subject)
                          .put("body", htmlBody);

                      return kafkaService
                          .sendMessage("email-service-topic-merchant-document-update-status",
                              String.valueOf(request.getMerchantId()), emailPayload)
                          .compose(v -> redisService.delete("merchant_document:" + updated.getId())).map(v -> updated)
                          .recover(
                              err -> redisService.delete("merchant_document:" + updated.getId()).map(v -> updated));
                    });
              });
        })
        .map(MerchantDocumentResponse::from)
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "updateMerchantDocumentStatus", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "updateMerchantDocumentStatus", e.getMessage()));
  }

  @Override
  public Future<MerchantDocumentResponseDeleteAt> trashedMerchantDocument(Integer documentId) {
    var ctx = tracingMetrics.startSpan("MerchantDocumentCommandService.trashedMerchantDocument");
    return repo.trashedMerchantDocument(documentId)
        .compose(doc -> {
          if (doc == null)
            return Future.failedFuture(new NotFoundException("Document not found"));
          return redisService.delete("merchant_document:" + documentId).map(v -> doc);
        })
        .map(MerchantDocumentResponseDeleteAt::from)
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "trashedMerchantDocument", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "trashedMerchantDocument", e.getMessage()));
  }

  @Override
  public Future<MerchantDocumentResponseDeleteAt> restoreMerchantDocument(Integer documentId) {
    var ctx = tracingMetrics.startSpan("MerchantDocumentCommandService.restoreMerchantDocument");

    return queryRepository.findByTrashedByIdDocument(documentId)
        .compose(trashed -> {
          if (trashed == null)
            return Future.failedFuture(new BadRequestException("Document not found or must be trashed first"));
          return repo.restoreMerchantDocument(documentId);
        })
        .compose(restored -> {
          if (restored == null)
            return Future.failedFuture(new NotFoundException("Document not found on restore"));
          return redisService.delete("merchant_document:" + documentId).map(v -> restored);
        })
        .map(MerchantDocumentResponseDeleteAt::from)
        .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "restoreMerchantDocument", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "restoreMerchantDocument", e.getMessage()));
  }

  @Override
  public Future<Void> deleteMerchantDocumentPermanent(Integer documentId) {
    var ctx = tracingMetrics.startSpan("MerchantDocumentCommandService.deleteMerchantDocumentPermanent",
        Attributes.builder().put("document.id", documentId).build());

    return queryRepository.findByTrashedByIdDocument(documentId)
        .compose(trashed -> {
          if (trashed == null)
            return Future.failedFuture(new BadRequestException("Document not found or must be trashed first"));
          return repo.deleteMerchantDocumentPermanent(documentId);
        })
        .compose(deleted -> {
          if (!deleted)
            return Future.failedFuture(new BadRequestException("Document not found or must be trashed first"));
          return redisService.delete("merchant_document:" + documentId).map(v -> (Void) null);
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "deleteMerchantDocumentPermanent", "Success"))
        .onFailure(e -> {
          logger.error("Failed to deletePermanent document: {}", documentId, e);
          tracingMetrics.completeSpanError(ctx, "deleteMerchantDocumentPermanent", e.getMessage());
        });
  }

  @Override
  public Future<Void> restoreAllMerchantDocument() {
    var ctx = tracingMetrics.startSpan("MerchantDocumentCommandService.restoreAllMerchantDocument");
    return repo.restoreAllMerchantDocuments()
        .compose(count -> {
          if (count == 0) {
            return Future.<Void>failedFuture(new NotFoundException("No trashed merchant documents found"));
          }
          return invalidateListCache();
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "restoreAllMerchantDocument", "Success"))
        .onFailure(e -> {
          logger.error("Failed to restore all merchant documents", e);
          tracingMetrics.completeSpanError(ctx, "restoreAllMerchantDocument", e.getMessage());
        });
  }

  @Override
  public Future<Void> deleteAllMerchantDocumentPermanent() {
    var ctx = tracingMetrics.startSpan("MerchantDocumentCommandService.deleteAllMerchantDocumentPermanent");
    return repo.deleteAllMerchantDocumentsPermanent()
        .compose(count -> {
          if (count == 0) {
            return Future.<Void>failedFuture(new NotFoundException("No trashed merchant documents found"));
          }
          return invalidateListCache();
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "deleteAllMerchantDocumentPermanent", "Success"))
        .onFailure(e -> {
          logger.error("Failed to permanently delete all merchant documents", e);
          tracingMetrics.completeSpanError(ctx, "deleteAllMerchantDocumentPermanent", e.getMessage());
        });
  }
}