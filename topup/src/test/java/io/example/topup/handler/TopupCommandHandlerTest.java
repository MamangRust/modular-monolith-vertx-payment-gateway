package io.example.topup.handler;

import com.google.protobuf.Empty;
import io.example.topup.model.TopupResponse;
import io.example.topup.model.TopupResponseDeleteAt;
import io.example.topup.service.TopupCommandService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.topup.Topup.FindByIdTopupRequest;
import pb.topup.TopupCommand.CreateTopupRequest;
import pb.topup.TopupCommand.UpdateTopupRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class TopupCommandHandlerTest {

  @Mock
  private TopupCommandService service;

  private TopupCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new TopupCommandHandler(service);
  }

  private static TopupResponse aResp(int id, String cardNumber, int amount) {
    return TopupResponse.builder()
        .id(id).cardNumber(cardNumber).amount(amount).status("success")
        .createdAt("2026-06-26T10:00:00Z").updatedAt("2026-06-26T10:00:00Z")
        .build();
  }

  private static TopupResponseDeleteAt aRespDel(int id, String cardNumber, int amount) {
    return TopupResponseDeleteAt.builder()
        .id(id).cardNumber(cardNumber).amount(amount).status("success")
        .createdAt("2026-06-26T10:00:00Z").updatedAt("2026-06-26T10:00:00Z")
        .build();
  }

  /* ─── createTopup ─── */

  @Test
  @DisplayName("createTopup delegates and returns response")
  void createTopup(VertxTestContext ctx) {
    when(service.createTopup(any())).thenReturn(Future.succeededFuture(aResp(1, "4111111111111111", 50000)));

    var req = CreateTopupRequest.newBuilder()
        .setCardNumber("4111111111111111").setTopupNo("TXN001")
        .setTopupAmount(50000).setTopupMethod("CREDIT_CARD")
        .build();

    handler.createTopup(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getId()).isEqualTo(1);
          assertThat(resp.getData().getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  /* ─── updateTopup ─── */

  @Test
  @DisplayName("updateTopup delegates and returns response")
  void updateTopup(VertxTestContext ctx) {
    when(service.updateTopup(any())).thenReturn(Future.succeededFuture(aResp(1, "4111111111111111", 75000)));

    var req = UpdateTopupRequest.newBuilder()
        .setTopupId(1).setCardNumber("4111111111111111")
        .setTopupAmount(75000).setTopupMethod("CREDIT_CARD")
        .build();

    handler.updateTopup(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── trashedTopup ─── */

  @Test
  @DisplayName("trashedTopup delegates and returns delete-at response")
  void trashedTopup(VertxTestContext ctx) {
    var trashed = aRespDel(1, "4111111111111111", 50000);
    trashed.setDeletedAt("2026-06-25T10:00:00Z");
    when(service.trashTopup(1)).thenReturn(Future.succeededFuture(trashed));

    var req = FindByIdTopupRequest.newBuilder().setTopupId(1).build();

    handler.trashedTopup(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getId()).isEqualTo(1);
          assertThat(resp.getData().hasDeletedAt()).isTrue();
          ctx.completeNow();
        })));
  }

  /* ─── restoreTopup ─── */

  @Test
  @DisplayName("restoreTopup delegates and returns delete-at response")
  void restoreTopup(VertxTestContext ctx) {
    when(service.restoreTopup(1)).thenReturn(Future.succeededFuture(aRespDel(1, "4111111111111111", 50000)));

    var req = FindByIdTopupRequest.newBuilder().setTopupId(1).build();

    handler.restoreTopup(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── deleteTopupPermanent ─── */

  @Test
  @DisplayName("deleteTopupPermanent delegates and returns success")
  void deleteTopupPermanent(VertxTestContext ctx) {
    when(service.deleteTopupPermanently(1)).thenReturn(Future.succeededFuture());

    var req = FindByIdTopupRequest.newBuilder().setTopupId(1).build();

    handler.deleteTopupPermanent(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  /* ─── restoreAllTopup ─── */

  @Test
  @DisplayName("restoreAllTopup delegates and returns success")
  void restoreAllTopup(VertxTestContext ctx) {
    when(service.restoreAllTopups()).thenReturn(Future.succeededFuture());

    handler.restoreAllTopup(Empty.getDefaultInstance())
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  /* ─── deleteAllTopupPermanent ─── */

  @Test
  @DisplayName("deleteAllTopupPermanent delegates and returns success")
  void deleteAllTopupPermanent(VertxTestContext ctx) {
    when(service.deleteAllPermanentTopups()).thenReturn(Future.succeededFuture());

    handler.deleteAllTopupPermanent(Empty.getDefaultInstance())
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  /* ─── error path ─── */

  @Test
  @DisplayName("createTopup delegates error when service fails")
  void createTopupError(VertxTestContext ctx) {
    when(service.createTopup(any()))
        .thenReturn(Future.failedFuture(new RuntimeException("DB error")));

    var req = CreateTopupRequest.newBuilder()
        .setCardNumber("4111111111111111").setTopupNo("TXN001")
        .setTopupAmount(50000).setTopupMethod("CREDIT_CARD")
        .build();

    handler.createTopup(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(io.grpc.StatusRuntimeException.class);
          ctx.completeNow();
        })));
  }
}
