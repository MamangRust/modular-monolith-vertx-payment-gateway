package io.example.transaction.handler;

import com.google.protobuf.Empty;
import io.example.transaction.model.TransactionResponse;
import io.example.transaction.model.TransactionResponseDeleteAt;
import io.example.transaction.service.TransactionCommandService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.transaction.Transaction.FindByIdTransactionRequest;
import pb.transaction.TransactionCommand.CreateTransactionRequest;
import pb.transaction.TransactionCommand.UpdateTransactionRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class TransactionCommandHandlerTest {

  @Mock
  private TransactionCommandService service;

  private TransactionCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new TransactionCommandHandler(service);
  }

  private static TransactionResponse aResp(int id, String cardNumber) {
    return TransactionResponse.builder()
        .id(id).cardNumber(cardNumber).amount(500_000).paymentMethod("CREDIT_CARD")
        .merchantId(1).status("success")
        .createdAt("2026-06-26T10:00:00Z").updatedAt("2026-06-26T10:00:00Z")
        .build();
  }

  private static TransactionResponseDeleteAt aRespDel(int id, String cardNumber) {
    return TransactionResponseDeleteAt.builder()
        .id(id).cardNumber(cardNumber).amount(500_000).paymentMethod("CREDIT_CARD")
        .merchantId(1).status("success")
        .createdAt("2026-06-26T10:00:00Z").updatedAt("2026-06-26T10:00:00Z")
        .build();
  }

  /* ─── createTransaction ─── */

  @Test
  @DisplayName("createTransaction delegates and returns response")
  void createTransaction(VertxTestContext ctx) {
    when(service.createTransaction(any())).thenReturn(Future.succeededFuture(aResp(1, "4111111111111111")));

    var req = CreateTransactionRequest.newBuilder()
        .setCardNumber("4111111111111111").setAmount(500_000).setPaymentMethod("CREDIT_CARD")
        .build();

    handler.createTransaction(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getId()).isEqualTo(1);
          assertThat(resp.getData().getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  /* ─── updateTransaction ─── */

  @Test
  @DisplayName("updateTransaction delegates and returns response")
  void updateTransaction(VertxTestContext ctx) {
    when(service.updateTransaction(any())).thenReturn(Future.succeededFuture(aResp(1, "4111111111111111")));

    var req = UpdateTransactionRequest.newBuilder()
        .setTransactionId(1).setCardNumber("4111111111111111").setAmount(500_000).build();

    handler.updateTransaction(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── trashedTransaction ─── */

  @Test
  @DisplayName("trashedTransaction delegates and returns delete-at response")
  void trashedTransaction(VertxTestContext ctx) {
    var trashed = aRespDel(1, "4111111111111111");
    trashed.setDeletedAt("2026-06-25T10:00:00Z");
    when(service.trashTransaction(1)).thenReturn(Future.succeededFuture(trashed));

    var req = FindByIdTransactionRequest.newBuilder().setTransactionId(1).build();

    handler.trashedTransaction(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getId()).isEqualTo(1);
          assertThat(resp.getData().hasDeletedAt()).isTrue();
          ctx.completeNow();
        })));
  }

  /* ─── restoreTransaction ─── */

  @Test
  @DisplayName("restoreTransaction delegates and returns delete-at response")
  void restoreTransaction(VertxTestContext ctx) {
    when(service.restoreTransaction(1)).thenReturn(Future.succeededFuture(aRespDel(1, "4111111111111111")));

    var req = FindByIdTransactionRequest.newBuilder().setTransactionId(1).build();

    handler.restoreTransaction(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── deleteTransactionPermanent ─── */

  @Test
  @DisplayName("deleteTransactionPermanent delegates and returns success")
  void deleteTransactionPermanent(VertxTestContext ctx) {
    when(service.deleteTransactionPermanently(1)).thenReturn(Future.succeededFuture());

    var req = FindByIdTransactionRequest.newBuilder().setTransactionId(1).build();

    handler.deleteTransactionPermanent(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  /* ─── restoreAllTransaction ─── */

  @Test
  @DisplayName("restoreAllTransaction delegates and returns success")
  void restoreAllTransaction(VertxTestContext ctx) {
    when(service.restoreAllTransactions()).thenReturn(Future.succeededFuture());

    handler.restoreAllTransaction(Empty.getDefaultInstance())
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  /* ─── deleteAllTransactionPermanent ─── */

  @Test
  @DisplayName("deleteAllTransactionPermanent delegates and returns success")
  void deleteAllTransactionPermanent(VertxTestContext ctx) {
    when(service.deleteAllPermanentTransactions()).thenReturn(Future.succeededFuture());

    handler.deleteAllTransactionPermanent(Empty.getDefaultInstance())
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  /* ─── error path ─── */

  @Test
  @DisplayName("createTransaction delegates error when service fails")
  void createTransactionError(VertxTestContext ctx) {
    when(service.createTransaction(any()))
        .thenReturn(Future.failedFuture(new RuntimeException("DB error")));

    var req = CreateTransactionRequest.newBuilder()
        .setCardNumber("4111111111111111").setAmount(500_000).setPaymentMethod("CREDIT_CARD")
        .build();

    handler.createTransaction(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(io.grpc.StatusRuntimeException.class);
          ctx.completeNow();
        })));
  }
}
