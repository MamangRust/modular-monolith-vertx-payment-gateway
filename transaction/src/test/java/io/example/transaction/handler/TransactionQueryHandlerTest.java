package io.example.transaction.handler;

import io.example.common.domain.ApiResponse;
import io.example.common.domain.ApiResponsePagination;
import io.example.common.domain.PaginationMeta;
import io.example.transaction.model.TransactionResponse;
import io.example.transaction.model.TransactionResponseDeleteAt;
import io.example.transaction.service.TransactionQueryService;
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
class TransactionQueryHandlerTest {

  @Mock
  private TransactionQueryService service;

  private TransactionQueryHandler handler;

  @BeforeEach
  void setUp() {
    handler = new TransactionQueryHandler(service);
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

  /* ─── findAllTransaction ─── */

  @Test
  @DisplayName("findAllTransaction returns paginated response")
  void findAllTransaction(VertxTestContext ctx) {
    var data = List.of(aResp(1, "4111111111111111"), aResp(2, "4222222222222222"));
    var meta = new PaginationMeta(1, 10, 1, 2);
    when(service.getTransactions(any()))
        .thenReturn(Future.succeededFuture(new ApiResponsePagination<>("success", "OK", data, meta)));

    var req = pb.transaction.TransactionQuery.FindAllTransactionRequest.newBuilder()
        .setPage(1).setPageSize(10).build();
    handler.findAllTransaction(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(2);
          assertThat(resp.getData(0).getCardNumber()).isEqualTo("4111111111111111");
          assertThat(resp.getPaginationMeta().getTotalRecords()).isEqualTo(2);
          ctx.completeNow();
        })));
  }

  /* ─── findAllTransactionByCardNumber ─── */

  @Test
  @DisplayName("findAllTransactionByCardNumber returns paginated response")
  void findAllTransactionByCardNumber(VertxTestContext ctx) {
    var data = List.of(aResp(1, "4111111111111111"));
    var meta = new PaginationMeta(1, 10, 1, 1);
    when(service.getTransactionsByCardNumber(any()))
        .thenReturn(Future.succeededFuture(new ApiResponsePagination<>("success", "OK", data, meta)));

    var req = pb.transaction.TransactionQuery.FindAllTransactionCardNumberRequest.newBuilder()
        .setCardNumber("4111111111111111").setPage(1).setPageSize(10).build();
    handler.findAllTransactionByCardNumber(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          assertThat(resp.getData(0).getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  /* ─── findByIdTransaction ─── */

  @Test
  @DisplayName("findByIdTransaction delegates and returns response")
  void findByIdTransaction(VertxTestContext ctx) {
    when(service.getTransactionById(1))
        .thenReturn(Future.succeededFuture(new ApiResponse<>("success", "OK", aResp(1, "4111111111111111"))));

    var req = pb.transaction.Transaction.FindByIdTransactionRequest.newBuilder().setTransactionId(1).build();
    handler.findByIdTransaction(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getId()).isEqualTo(1);
          assertThat(resp.getData().getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  /* ─── findTransactionByMerchantId ─── */

  @Test
  @DisplayName("findTransactionByMerchantId returns list of transactions")
  void findTransactionByMerchantId(VertxTestContext ctx) {
    var data = List.of(aResp(1, "4111111111111111"));
    when(service.getTransactionsByMerchantId(1))
        .thenReturn(Future.succeededFuture(new ApiResponse<>("success", "OK", data)));

    var req = pb.transaction.TransactionQuery.FindTransactionByMerchantIdRequest.newBuilder().setMerchantId(1).build();
    handler.findTransactionByMerchantId(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── findByActiveTransaction ─── */

  @Test
  @DisplayName("findByActiveTransaction returns paginated active transactions")
  void findByActiveTransaction(VertxTestContext ctx) {
    var data = List.of(aRespDel(1, "4111111111111111"));
    var meta = new PaginationMeta(1, 10, 1, 1);
    when(service.getActiveTransactions(any()))
        .thenReturn(Future.succeededFuture(new ApiResponsePagination<>("success", "OK", data, meta)));

    var req = pb.transaction.TransactionQuery.FindAllTransactionRequest.newBuilder()
        .setPage(1).setPageSize(10).build();
    handler.findByActiveTransaction(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── findByTrashedTransaction ─── */

  @Test
  @DisplayName("findByTrashedTransaction returns paginated trashed transactions")
  void findByTrashedTransaction(VertxTestContext ctx) {
    var data = List.of(aRespDel(1, "4111111111111111"));
    var meta = new PaginationMeta(1, 10, 1, 1);
    when(service.getTrashedTransactions(any()))
        .thenReturn(Future.succeededFuture(new ApiResponsePagination<>("success", "OK", data, meta)));

    var req = pb.transaction.TransactionQuery.FindAllTransactionRequest.newBuilder()
        .setPage(1).setPageSize(10).build();
    handler.findByTrashedTransaction(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }
}
