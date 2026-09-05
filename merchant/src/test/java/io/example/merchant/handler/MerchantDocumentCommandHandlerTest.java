package io.example.merchant.handler;

import com.google.protobuf.Empty;
import io.example.merchant.model.MerchantDocumentResponse;
import io.example.merchant.model.MerchantDocumentResponseDeleteAt;
import io.example.merchant.service.MerchantDocumentCommandService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.merchant_document.MerchantDocumentCommand.CreateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest;
import pb.merchant_document.MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({ MockitoExtension.class, VertxExtension.class })
class MerchantDocumentCommandHandlerTest {

  @Mock
  private MerchantDocumentCommandService service;

  private MerchantDocumentCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new MerchantDocumentCommandHandler(service);
  }

  private static MerchantDocumentResponse aResp(int id) {
    return MerchantDocumentResponse.builder()
        .id(id).merchantId(10).documentType("ID_CARD").documentUrl("http://url")
        .status("pending").note("").createdAt("2026-06-26T10:00:00Z")
        .updatedAt("2026-06-26T10:00:00Z").build();
  }

  private static MerchantDocumentResponseDeleteAt aRespDeleteAt(int id) {
    return MerchantDocumentResponseDeleteAt.builder()
        .id(id).merchantId(10).documentType("ID_CARD").documentUrl("http://url")
        .status("pending").note("").createdAt("2026-06-26T10:00:00Z")
        .updatedAt("2026-06-26T10:00:00Z").deletedAt("2026-06-26T10:05:00Z").build();
  }

  @Test
  @DisplayName("create handles request successfully")
  void createSuccess(VertxTestContext ctx) {
    when(service.createMerchantDocument(any())).thenReturn(Future.succeededFuture(aResp(100)));

    var req = CreateMerchantDocumentRequest.newBuilder().setMerchantId(10).build();
    handler.create(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getData().getDocumentId()).isEqualTo(100);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("update handles request successfully")
  void updateSuccess(VertxTestContext ctx) {
    when(service.updateMerchantDocument(any())).thenReturn(Future.succeededFuture(aResp(100)));

    var req = UpdateMerchantDocumentRequest.newBuilder().setDocumentId(100).build();
    handler.update(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getData().getDocumentId()).isEqualTo(100);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateStatus handles request successfully")
  void updateStatusSuccess(VertxTestContext ctx) {
    when(service.updateMerchantDocumentStatus(any())).thenReturn(Future.succeededFuture(aResp(100)));

    var req = UpdateMerchantDocumentStatusRequest.newBuilder().setDocumentId(100).build();
    handler.updateStatus(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getData().getDocumentId()).isEqualTo(100);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("trashed handles request successfully")
  void trashedSuccess(VertxTestContext ctx) {
    when(service.trashedMerchantDocument(100)).thenReturn(Future.succeededFuture(aRespDeleteAt(100)));

    var req = FindMerchantDocumentByIdRequest.newBuilder().setDocumentId(100).build();
    handler.trashed(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getData().getDocumentId()).isEqualTo(100);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("restore handles request successfully")
  void restoreSuccess(VertxTestContext ctx) {
    when(service.restoreMerchantDocument(100)).thenReturn(Future.succeededFuture(aRespDeleteAt(100)));

    var req = FindMerchantDocumentByIdRequest.newBuilder().setDocumentId(100).build();
    handler.restore(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getData().getDocumentId()).isEqualTo(100);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deletePermanent handles request successfully")
  void deletePermanentSuccess(VertxTestContext ctx) {
    when(service.deleteMerchantDocumentPermanent(100)).thenReturn(Future.succeededFuture());

    var req = FindMerchantDocumentByIdRequest.newBuilder().setDocumentId(100).build();
    handler.deletePermanent(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("restoreAll handles request successfully")
  void restoreAllSuccess(VertxTestContext ctx) {
    when(service.restoreAllMerchantDocument()).thenReturn(Future.succeededFuture());

    handler.restoreAll(Empty.getDefaultInstance())
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deleteAllPermanent handles request successfully")
  void deleteAllPermanentSuccess(VertxTestContext ctx) {
    when(service.deleteAllMerchantDocumentPermanent()).thenReturn(Future.succeededFuture());

    handler.deleteAllPermanent(Empty.getDefaultInstance())
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }
}
