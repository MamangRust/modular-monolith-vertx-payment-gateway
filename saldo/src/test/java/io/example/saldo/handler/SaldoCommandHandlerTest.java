package io.example.saldo.handler;

import com.google.protobuf.Empty;
import io.example.saldo.model.SaldoResponse;
import io.example.saldo.model.SaldoResponseDeleteAt;
import io.example.saldo.service.SaldoCommandService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.saldo.Saldo.FindByIdSaldoRequest;
import pb.saldo.SaldoCommand.CreateSaldoRequest;
import pb.saldo.SaldoCommand.UpdateSaldoBalanceRequest;
import pb.saldo.SaldoCommand.UpdateSaldoRequest;
import pb.saldo.SaldoCommand.UpdateSaldoWithdrawRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class SaldoCommandHandlerTest {

  @Mock
  private SaldoCommandService service;

  private SaldoCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new SaldoCommandHandler(service);
  }

  private static SaldoResponse aResp(int id, String cardNumber) {
    return SaldoResponse.builder()
        .id(id)
        .cardNumber(cardNumber)
        .totalBalance(1_000_000L)
        .createdAt("2026-06-26T10:00:00Z")
        .updatedAt("2026-06-26T10:00:00Z")
        .build();
  }

  private static SaldoResponseDeleteAt aRespDel(int id, String cardNumber) {
    return SaldoResponseDeleteAt.builder()
        .id(id)
        .cardNumber(cardNumber)
        .totalBalance(1_000_000L)
        .createdAt("2026-06-26T10:00:00Z")
        .updatedAt("2026-06-26T10:00:00Z")
        .build();
  }

  /* ─── createSaldo ─── */

  @Test
  @DisplayName("createSaldo delegates and returns response")
  void createSaldo(VertxTestContext ctx) {
    when(service.createSaldo(any())).thenReturn(Future.succeededFuture(aResp(1, "4111111111111111")));

    var req = CreateSaldoRequest.newBuilder()
        .setCardNumber("4111111111111111")
        .setTotalBalance(1_000_000)
        .build();

    handler.createSaldo(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getSaldoId()).isEqualTo(1);
          assertThat(resp.getData().getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  /* ─── updateSaldo ─── */

  @Test
  @DisplayName("updateSaldo delegates and returns response")
  void updateSaldo(VertxTestContext ctx) {
    when(service.updateSaldo(any())).thenReturn(Future.succeededFuture(aResp(1, "4111111111111111")));

    var req = UpdateSaldoRequest.newBuilder()
        .setSaldoId(1)
        .setCardNumber("4111111111111111")
        .setTotalBalance(2_000_000)
        .build();

    handler.updateSaldo(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getSaldoId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── updateSaldoBalance ─── */

  @Test
  @DisplayName("updateSaldoBalance delegates and returns response")
  void updateSaldoBalance(VertxTestContext ctx) {
    when(service.updateSaldoBalance(any())).thenReturn(Future.succeededFuture(aResp(1, "4111111111111111")));

    var req = UpdateSaldoBalanceRequest.newBuilder()
        .setCardNumber("4111111111111111")
        .setTotalBalance(500_000)
        .build();

    handler.updateSaldoBalance(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getTotalBalance()).isEqualTo(1_000_000);
          ctx.completeNow();
        })));
  }

  /* ─── updateSaldoWithdraw ─── */

  @Test
  @DisplayName("updateSaldoWithdraw delegates and returns response")
  void updateSaldoWithdraw(VertxTestContext ctx) {
    when(service.updateSaldoWithdraw(any())).thenReturn(Future.succeededFuture(aResp(1, "4111111111111111")));

    var req = UpdateSaldoWithdrawRequest.newBuilder()
        .setCardNumber("4111111111111111")
        .setWithdrawAmount(100_000)
        .setWithdrawTime("2026-06-26T10:00:00")
        .build();

    handler.updateSaldoWithdraw(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getSaldoId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── trashedSaldo ─── */

  @Test
  @DisplayName("trashedSaldo delegates and returns delete-at response")
  void trashedSaldo(VertxTestContext ctx) {
    when(service.trashSaldo(1)).thenReturn(Future.succeededFuture(aRespDel(1, "4111111111111111")));

    var req = FindByIdSaldoRequest.newBuilder().setSaldoId(1).build();

    handler.trashedSaldo(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getSaldoId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── restoreSaldo ─── */

  @Test
  @DisplayName("restoreSaldo delegates and returns delete-at response")
  void restoreSaldo(VertxTestContext ctx) {
    when(service.restoreSaldo(1)).thenReturn(Future.succeededFuture(aRespDel(1, "4111111111111111")));

    var req = FindByIdSaldoRequest.newBuilder().setSaldoId(1).build();

    handler.restoreSaldo(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getSaldoId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── deleteSaldoPermanent ─── */

  @Test
  @DisplayName("deleteSaldoPermanent delegates and returns success")
  void deleteSaldoPermanent(VertxTestContext ctx) {
    when(service.deleteSaldoPermanently(1)).thenReturn(Future.succeededFuture());

    var req = FindByIdSaldoRequest.newBuilder().setSaldoId(1).build();

    handler.deleteSaldoPermanent(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getMessage()).isEqualTo("Saldo deleted permanently");
          ctx.completeNow();
        })));
  }

  /* ─── restoreAllSaldo ─── */

  @Test
  @DisplayName("restoreAllSaldo delegates and returns success")
  void restoreAllSaldo(VertxTestContext ctx) {
    when(service.restoreAllSaldos()).thenReturn(Future.succeededFuture());

    handler.restoreAllSaldo(Empty.getDefaultInstance())
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getMessage()).isEqualTo("All saldos restored successfully");
          ctx.completeNow();
        })));
  }

  /* ─── deleteAllSaldoPermanent ─── */

  @Test
  @DisplayName("deleteAllSaldoPermanent delegates and returns success")
  void deleteAllSaldoPermanent(VertxTestContext ctx) {
    when(service.deleteAllPermanentSaldos()).thenReturn(Future.succeededFuture());

    handler.deleteAllSaldoPermanent(Empty.getDefaultInstance())
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getMessage()).isEqualTo("All saldos permanently deleted");
          ctx.completeNow();
        })));
  }

  /* ─── error path ─── */

  @Test
  @DisplayName("createSaldo delegates error when service fails")
  void createSaldoError(VertxTestContext ctx) {
    when(service.createSaldo(any()))
        .thenReturn(Future.failedFuture(new RuntimeException("DB error")));

    var req = CreateSaldoRequest.newBuilder().setCardNumber("4111111111111111").build();

    handler.createSaldo(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(io.grpc.StatusRuntimeException.class);
          ctx.completeNow();
        })));
  }
}
