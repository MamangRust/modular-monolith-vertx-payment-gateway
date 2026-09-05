package io.example.withdraw.handler;

import io.example.common.domain.PagedResult;
import io.example.withdraw.model.WithdrawResponse;
import io.example.withdraw.model.WithdrawResponseDeleteAt;
import io.example.withdraw.service.WithdrawQueryService;
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
class WithdrawQueryHandlerTest {

  @Mock
  private WithdrawQueryService service;

  private WithdrawQueryHandler handler;

  @BeforeEach
  void setUp() {
    handler = new WithdrawQueryHandler(service);
  }

  private static WithdrawResponse aResp(int id, String cardNumber) {
    return WithdrawResponse.builder().id(id).cardNumber(cardNumber)
        .withdrawAmount(500_000).status("success")
        .createdAt("2026-06-26T10:00:00Z").updatedAt("2026-06-26T10:00:00Z")
        .build();
  }

  private static WithdrawResponseDeleteAt aRespDeleteAt(int id, String cardNumber) {
    return WithdrawResponseDeleteAt.builder().id(id).cardNumber(cardNumber)
        .withdrawAmount(500_000).status("success")
        .createdAt("2026-06-26T10:00:00Z").updatedAt("2026-06-26T10:00:00Z")
        .build();
  }

  @Test
  @DisplayName("findAllWithdraw returns paginated response")
  void findAllWithdraw(VertxTestContext ctx) {
    var data = List.of(aResp(1, "4111-1111"), aResp(2, "4111-2222"));
    when(service.getWithdraws(any())).thenReturn(Future.succeededFuture(new PagedResult<>(data, 2)));

    var req = pb.withdraw.Withdraw.FindAllWithdrawRequest.newBuilder().setPage(1).setPageSize(10).build();
    handler.findAllWithdraw(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(2);
          assertThat(resp.getData(0).getCardNumber()).isEqualTo("4111-1111");
          assertThat(resp.getPaginationMeta().getTotalRecords()).isEqualTo(2);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findAllWithdrawByCardNumber returns paginated response")
  void findAllWithdrawByCardNumber(VertxTestContext ctx) {
    var data = List.of(aResp(1, "4111-1111"));
    when(service.getWithdrawsByCardNumber(any())).thenReturn(Future.succeededFuture(new PagedResult<>(data, 1)));

    var req = pb.withdraw.Withdraw.FindAllWithdrawByCardNumberRequest.newBuilder()
        .setCardNumber("4111-1111").setPage(1).setPageSize(10).build();
    handler.findAllWithdrawByCardNumber(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          assertThat(resp.getData(0).getCardNumber()).isEqualTo("4111-1111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByIdWithdraw returns withdraw response")
  void findByIdWithdraw(VertxTestContext ctx) {
    when(service.getWithdrawById(1)).thenReturn(Future.succeededFuture(aResp(1, "4111-1111")));

    var req = pb.withdraw.Withdraw.FindByIdWithdrawRequest.newBuilder().setWithdrawId(1).build();
    handler.findByIdWithdraw(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getCardNumber()).isEqualTo("4111-1111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByCardNumber returns list of withdraws")
  void findByCardNumber(VertxTestContext ctx) {
    when(service.getWithdrawsByCardNumberPrimitive("4111-1111"))
        .thenReturn(Future.succeededFuture(List.of(aResp(1, "4111-1111"))));

    var req = pb.card.Card.FindByCardNumberRequest.newBuilder().setCardNumber("4111-1111").build();
    handler.findByCardNumber(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByActive returns paginated active withdraws")
  void findByActive(VertxTestContext ctx) {
    var data = List.of(aRespDeleteAt(1, "4111-1111"));
    when(service.getActiveWithdraws(any())).thenReturn(Future.succeededFuture(new PagedResult<>(data, 1)));

    var req = pb.withdraw.Withdraw.FindAllWithdrawRequest.newBuilder().setPage(1).setPageSize(10).build();
    handler.findByActive(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByTrashed returns paginated trashed withdraws")
  void findByTrashed(VertxTestContext ctx) {
    var data = List.of(aRespDeleteAt(1, "4111-1111"));
    when(service.getTrashedWithdraws(any())).thenReturn(Future.succeededFuture(new PagedResult<>(data, 1)));

    var req = pb.withdraw.Withdraw.FindAllWithdrawRequest.newBuilder().setPage(1).setPageSize(10).build();
    handler.findByTrashed(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }
}
