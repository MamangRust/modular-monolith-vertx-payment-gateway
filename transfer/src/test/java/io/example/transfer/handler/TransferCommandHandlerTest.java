package io.example.transfer.handler;

import com.google.protobuf.Empty;
import io.example.transfer.model.TransferResponse;
import io.example.transfer.model.TransferResponseDeleteAt;
import io.example.transfer.service.TransferCommandService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.transfer.Transfer.FindByIdTransferRequest;
import pb.transfer.TransferCommand.CreateTransferRequest;
import pb.transfer.TransferCommand.UpdateTransferRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class TransferCommandHandlerTest {

  @Mock
  private TransferCommandService service;

  private TransferCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new TransferCommandHandler(service);
  }

  private static TransferResponse aResp(int id, String cardNumber) {
    return TransferResponse.builder().id(id).transferNo("TRF" + id)
        .transferFrom("4111111111111111").transferTo("5555555555554444")
        .transferAmount(500_000L).status("success")
        .createdAt("2026-06-26T10:00:00Z").updatedAt("2026-06-26T10:00:00Z")
        .build();
  }

  private static TransferResponseDeleteAt aRespDel(int id, String cardNumber) {
    return TransferResponseDeleteAt.builder().id(id).transferNo("TRF" + id)
        .transferFrom("4111111111111111").transferTo("5555555555554444")
        .transferAmount(500_000L).status("success")
        .createdAt("2026-06-26T10:00:00Z").updatedAt("2026-06-26T10:00:00Z")
        .build();
  }

  /* ─── createTransfer ─── */

  @Test
  @DisplayName("createTransfer delegates and returns response")
  void createTransfer(VertxTestContext ctx) {
    when(service.createTransfer(any())).thenReturn(Future.succeededFuture(aResp(1, "4111111111111111")));

    var req = CreateTransferRequest.newBuilder()
        .setTransferFrom("4111111111111111")
        .setTransferTo("5555555555554444")
        .setTransferAmount(500_000)
        .build();
    handler.createTransfer(req)
        .<pb.transfer.Transfer.ApiResponseTransfer>onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getId()).isEqualTo(1);
          assertThat(resp.getData().getTransferFrom()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  /* ─── updateTransfer ─── */

  @Test
  @DisplayName("updateTransfer delegates and returns response")
  void updateTransfer(VertxTestContext ctx) {
    when(service.updateTransfer(any())).thenReturn(Future.succeededFuture(aResp(1, "4111111111111111")));

    var req = UpdateTransferRequest.newBuilder().setTransferId(1).build();

    handler.updateTransfer(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── trashedTransfer ─── */

  @Test
  @DisplayName("trashedTransfer delegates and returns delete-at response")
  void trashedTransfer(VertxTestContext ctx) {
    when(service.trashTransfer(1)).thenReturn(Future.succeededFuture(aRespDel(1, "4111111111111111")));

    var req = FindByIdTransferRequest.newBuilder().setTransferId(1).build();

    handler.trashedTransfer(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── restoreTransfer ─── */

  @Test
  @DisplayName("restoreTransfer delegates and returns delete-at response")
  void restoreTransfer(VertxTestContext ctx) {
    when(service.restoreTransfer(1)).thenReturn(Future.succeededFuture(aRespDel(1, "4111111111111111")));

    var req = FindByIdTransferRequest.newBuilder().setTransferId(1).build();

    handler.restoreTransfer(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── deleteTransferPermanent ─── */

  @Test
  @DisplayName("deleteTransferPermanent delegates and returns success")
  void deleteTransferPermanent(VertxTestContext ctx) {
    when(service.deleteTransferPermanently(1)).thenReturn(Future.succeededFuture());

    var req = FindByIdTransferRequest.newBuilder().setTransferId(1).build();

    handler.deleteTransferPermanent(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  /* ─── restoreAllTransfer ─── */

  @Test
  @DisplayName("restoreAllTransfer delegates and returns success")
  void restoreAllTransfer(VertxTestContext ctx) {
    when(service.restoreAllTransfers()).thenReturn(Future.succeededFuture());

    handler.restoreAllTransfer(Empty.getDefaultInstance())
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  /* ─── deleteAllTransferPermanent ─── */

  @Test
  @DisplayName("deleteAllTransferPermanent delegates and returns success")
  void deleteAllTransferPermanent(VertxTestContext ctx) {
    when(service.deleteAllPermanentTransfers()).thenReturn(Future.succeededFuture());

    handler.deleteAllTransferPermanent(Empty.getDefaultInstance())
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  /* ─── error path ─── */

  @Test
  @DisplayName("createTransfer delegates error when service fails")
  void createTransferError(VertxTestContext ctx) {
    when(service.createTransfer(any()))
        .thenReturn(Future.failedFuture(new RuntimeException("DB error")));

    var req = CreateTransferRequest.newBuilder()
        .setTransferFrom("4111111111111111")
        .setTransferTo("5555555555554444")
        .setTransferAmount(500_000)
        .build();

    handler.createTransfer(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(io.grpc.StatusRuntimeException.class);
          ctx.completeNow();
        })));
  }
}
