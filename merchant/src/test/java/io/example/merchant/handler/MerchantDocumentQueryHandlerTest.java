package io.example.merchant.handler;

import io.example.common.domain.PagedResult;
import io.example.merchant.model.MerchantDocumentResponse;
import io.example.merchant.model.MerchantDocumentResponseDeleteAt;
import io.example.merchant.service.MerchantDocumentQueryService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.merchant_document.MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest;
import pb.merchant_document.MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({ MockitoExtension.class, VertxExtension.class })
class MerchantDocumentQueryHandlerTest {

  @Mock
  private MerchantDocumentQueryService service;

  private MerchantDocumentQueryHandler handler;

  @BeforeEach
  void setUp() {
    handler = new MerchantDocumentQueryHandler(service);
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
  @DisplayName("findAll returns pagination response")
  void findAllSuccess(VertxTestContext ctx) {
    var paged = new PagedResult<>(List.of(aResp(100)), 1);
    when(service.findAll(any())).thenReturn(Future.succeededFuture(paged));

    var req = FindAllMerchantDocumentsRequest.newBuilder().setPage(1).setPageSize(10).build();
    handler.findAll(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          assertThat(res.getData(0).getDocumentId()).isEqualTo(100);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findAllActive returns pagination response")
  void findAllActiveSuccess(VertxTestContext ctx) {
    var paged = new PagedResult<>(List.of(aRespDeleteAt(100)), 1);
    when(service.findByActive(any())).thenReturn(Future.succeededFuture(paged));

    var req = FindAllMerchantDocumentsRequest.newBuilder().setPage(1).setPageSize(10).build();
    handler.findAllActive(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findAllTrashed returns pagination response")
  void findAllTrashedSuccess(VertxTestContext ctx) {
    var paged = new PagedResult<>(List.of(aRespDeleteAt(100)), 1);
    when(service.findByTrashed(any())).thenReturn(Future.succeededFuture(paged));

    var req = FindAllMerchantDocumentsRequest.newBuilder().setPage(1).setPageSize(10).build();
    handler.findAllTrashed(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findById returns single document response")
  void findByIdSuccess(VertxTestContext ctx) {
    when(service.findById(100)).thenReturn(Future.succeededFuture(aResp(100)));

    var req = FindMerchantDocumentByIdRequest.newBuilder().setDocumentId(100).build();
    handler.findById(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getData().getDocumentId()).isEqualTo(100);
          ctx.completeNow();
        })));
  }
}
