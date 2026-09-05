package io.example.merchant.handler;

import io.example.common.domain.PagedResult;
import io.example.merchant.model.MerchantResponse;
import io.example.merchant.model.MerchantResponseDeleteAt;
import io.example.merchant.service.MerchantQueryService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class MerchantQueryHandlerTest {

  @Mock
  private MerchantQueryService service;

  private MerchantQueryHandler handler;

  @BeforeEach
  void setUp() {
    handler = new MerchantQueryHandler(service);
  }

  private static MerchantResponse aResp(int id, String name) {
    return MerchantResponse.builder()
        .id(id).name(name).apiKey("key_" + id).userId(1).status("active")
        .createdAt("2026-06-26T10:00:00Z").updatedAt("2026-06-26T10:00:00Z")
        .build();
  }

  private static MerchantResponseDeleteAt aRespDeleteAt(int id, String name) {
    return MerchantResponseDeleteAt.builder()
        .id(id).name(name).apiKey("key_" + id).userId(1).status("active")
        .createdAt("2026-06-26T10:00:00Z").updatedAt("2026-06-26T10:00:00Z")
        .build();
  }

  @Test
  @DisplayName("findAllMerchant returns paginated response")
  void findAllMerchant(VertxTestContext ctx) {
    var data = List.of(aResp(1, "Merchant A"), aResp(2, "Merchant B"));
    when(service.findAll(any())).thenReturn(Future.succeededFuture(new PagedResult<>(data, 2)));

    var req = pb.merchant.Merchant.FindAllMerchantRequest.newBuilder().setPage(1).setPageSize(10).build();
    handler.findAllMerchant(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(2);
          assertThat(resp.getData(0).getName()).isEqualTo("Merchant A");
          assertThat(resp.getPaginationMeta().getTotalRecords()).isEqualTo(2);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByIdMerchant returns merchant response")
  void findByIdMerchant(VertxTestContext ctx) {
    when(service.findById(1)).thenReturn(Future.succeededFuture(aResp(1, "Single Merchant")));

    var req = pb.merchant.Merchant.FindByIdMerchantRequest.newBuilder().setMerchantId(1).build();
    handler.findByIdMerchant(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getName()).isEqualTo("Single Merchant");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByApiKey returns merchant response")
  void findByApiKey(VertxTestContext ctx) {
    when(service.findByApiKey("key_abc")).thenReturn(Future.succeededFuture(aResp(1, "ApiKey Merchant")));

    var req = pb.merchant.Merchant.FindByApiKeyRequest.newBuilder().setApiKey("key_abc").build();
    handler.findByApiKey(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getName()).isEqualTo("ApiKey Merchant");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByMerchantUserId returns list of merchants")
  void findByMerchantUserId(VertxTestContext ctx) {
    when(service.findByMerchantUserId(1)).thenReturn(Future.succeededFuture(List.of(aResp(1, "User Merchant"))));

    var req = pb.merchant.Merchant.FindByMerchantUserIdRequest.newBuilder().setUserId(1).build();
    handler.findByMerchantUserId(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByActive returns paginated active merchants")
  void findByActive(VertxTestContext ctx) {
    var data = List.of(aRespDeleteAt(1, "Active Merchant"));
    when(service.findByActive(any())).thenReturn(Future.succeededFuture(new PagedResult<>(data, 1)));

    var req = pb.merchant.Merchant.FindAllMerchantRequest.newBuilder().setPage(1).setPageSize(10).build();
    handler.findByActive(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByTrashed returns paginated trashed merchants")
  void findByTrashed(VertxTestContext ctx) {
    var data = List.of(aRespDeleteAt(1, "Trashed Merchant"));
    when(service.findByTrashed(any())).thenReturn(Future.succeededFuture(new PagedResult<>(data, 1)));

    var req = pb.merchant.Merchant.FindAllMerchantRequest.newBuilder().setPage(1).setPageSize(10).build();
    handler.findByTrashed(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }
}
