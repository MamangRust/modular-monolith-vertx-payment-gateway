package io.example.merchant.handler;

import com.google.protobuf.Empty;
import io.example.merchant.model.MerchantResponse;
import io.example.merchant.model.MerchantResponseDeleteAt;
import io.example.merchant.service.MerchantCommandService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.merchant.Merchant.FindByIdMerchantRequest;
import pb.merchant.MerchantCommand.CreateMerchantRequest;
import pb.merchant.MerchantCommand.UpdateMerchantRequest;
import pb.merchant.MerchantCommand.UpdateMerchantStatusRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class MerchantCommandHandlerTest {

  @Mock
  private MerchantCommandService service;

  private MerchantCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new MerchantCommandHandler(service);
  }

  private static MerchantResponse aResp(int id, String name) {
    return MerchantResponse.builder()
        .id(id).name(name).apiKey("key_" + id).userId(1).status("active")
        .createdAt("2026-06-26T10:00:00Z").updatedAt("2026-06-26T10:00:00Z")
        .build();
  }

  private static MerchantResponseDeleteAt aRespDel(int id, String name) {
    return MerchantResponseDeleteAt.builder()
        .id(id).name(name).apiKey("key_" + id).userId(1).status("active")
        .createdAt("2026-06-26T10:00:00Z").updatedAt("2026-06-26T10:00:00Z")
        .build();
  }

  @Test
  @DisplayName("createMerchant delegates and returns response")
  void createMerchant(VertxTestContext ctx) {
    when(service.createMerchant(any())).thenReturn(Future.succeededFuture(aResp(1, "New")));

    var req = CreateMerchantRequest.newBuilder().setName("New").setUserId(1).build();
    handler.createMerchant(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getName()).isEqualTo("New");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateMerchant delegates and returns response")
  void updateMerchant(VertxTestContext ctx) {
    when(service.updateMerchant(any())).thenReturn(Future.succeededFuture(aResp(1, "Updated")));

    var req = UpdateMerchantRequest.newBuilder().setMerchantId(1).setName("Updated").build();
    handler.updateMerchant(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getName()).isEqualTo("Updated");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateMerchantStatus delegates and returns response")
  void updateMerchantStatus(VertxTestContext ctx) {
    when(service.updateMerchantStatus(any())).thenReturn(Future.succeededFuture(aResp(1, "Active")));

    var req = UpdateMerchantStatusRequest.newBuilder().setMerchantId(1).setStatus("active").build();
    handler.updateMerchantStatus(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("trashedMerchant delegates and returns delete-at response")
  void trashedMerchant(VertxTestContext ctx) {
    when(service.trashedMerchant(1)).thenReturn(Future.succeededFuture(aRespDel(1, "Trashed")));

    var req = FindByIdMerchantRequest.newBuilder().setMerchantId(1).build();
    handler.trashedMerchant(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getName()).isEqualTo("Trashed");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("restoreMerchant delegates and returns response")
  void restoreMerchant(VertxTestContext ctx) {
    when(service.restoreMerchant(1)).thenReturn(Future.succeededFuture(aRespDel(1, "Restored")));

    var req = FindByIdMerchantRequest.newBuilder().setMerchantId(1).build();
    handler.restoreMerchant(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deleteMerchantPermanent delegates and returns success")
  void deleteMerchantPermanent(VertxTestContext ctx) {
    when(service.deleteMerchantPermanent(1)).thenReturn(Future.succeededFuture());

    var req = FindByIdMerchantRequest.newBuilder().setMerchantId(1).build();
    handler.deleteMerchantPermanent(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("restoreAllMerchant delegates and returns success")
  void restoreAllMerchant(VertxTestContext ctx) {
    when(service.restoreAllMerchant()).thenReturn(Future.succeededFuture());

    handler.restoreAllMerchant(Empty.getDefaultInstance())
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deleteAllMerchantPermanent delegates and returns success")
  void deleteAllMerchantPermanent(VertxTestContext ctx) {
    when(service.deleteAllMerchantPermanent()).thenReturn(Future.succeededFuture());

    handler.deleteAllMerchantPermanent(Empty.getDefaultInstance())
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }
}
