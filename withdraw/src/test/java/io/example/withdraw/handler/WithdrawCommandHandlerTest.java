package io.example.withdraw.handler;

import com.google.protobuf.Empty;
import io.example.withdraw.model.WithdrawResponse;
import io.example.withdraw.model.WithdrawResponseDeleteAt;
import io.example.withdraw.service.WithdrawCommandService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.withdraw.Withdraw.FindByIdWithdrawRequest;
import pb.withdraw.WithdrawCommand.CreateWithdrawRequest;
import pb.withdraw.WithdrawCommand.UpdateWithdrawRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class WithdrawCommandHandlerTest {

  @Mock
  private WithdrawCommandService service;

  private WithdrawCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new WithdrawCommandHandler(service);
  }

  private static WithdrawResponse aResp(int id, String cardNumber) {
    return WithdrawResponse.builder().id(id).cardNumber(cardNumber)
        .withdrawAmount(500_000).status("success")
        .createdAt("2026-06-26T10:00:00Z").updatedAt("2026-06-26T10:00:00Z")
        .build();
  }

  private static WithdrawResponseDeleteAt aRespDel(int id, String cardNumber) {
    return WithdrawResponseDeleteAt.builder().id(id).cardNumber(cardNumber)
        .withdrawAmount(500_000).status("success")
        .createdAt("2026-06-26T10:00:00Z").updatedAt("2026-06-26T10:00:00Z")
        .build();
  }

  @Test
  @DisplayName("createWithdraw delegates and returns response")
  void createWithdraw(VertxTestContext ctx) {
    when(service.createWithdraw(any())).thenReturn(Future.succeededFuture(aResp(1, "4111-1111")));

    var req = CreateWithdrawRequest.newBuilder().setCardNumber("4111-1111").setWithdrawAmount(500_000).build();
    handler.createWithdraw(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getCardNumber()).isEqualTo("4111-1111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateWithdraw delegates and returns response")
  void updateWithdraw(VertxTestContext ctx) {
    when(service.updateWithdraw(any())).thenReturn(Future.succeededFuture(aResp(1, "4111-1111")));

    var req = UpdateWithdrawRequest.newBuilder().setWithdrawId(1).setCardNumber("4111-1111").setWithdrawAmount(500_000).build();
    handler.updateWithdraw(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getCardNumber()).isEqualTo("4111-1111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("trashedWithdraw delegates and returns delete-at response")
  void trashedWithdraw(VertxTestContext ctx) {
    when(service.trashWithdraw(1)).thenReturn(Future.succeededFuture(aRespDel(1, "4111-1111")));

    var req = FindByIdWithdrawRequest.newBuilder().setWithdrawId(1).build();
    handler.trashedWithdraw(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getCardNumber()).isEqualTo("4111-1111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("restoreWithdraw delegates and returns response")
  void restoreWithdraw(VertxTestContext ctx) {
    when(service.restoreWithdraw(1)).thenReturn(Future.succeededFuture(aRespDel(1, "4111-1111")));

    var req = FindByIdWithdrawRequest.newBuilder().setWithdrawId(1).build();
    handler.restoreWithdraw(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getCardNumber()).isEqualTo("4111-1111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deleteWithdrawPermanent delegates and returns success")
  void deleteWithdrawPermanent(VertxTestContext ctx) {
    when(service.deleteWithdrawPermanently(1)).thenReturn(Future.succeededFuture());

    var req = FindByIdWithdrawRequest.newBuilder().setWithdrawId(1).build();
    handler.deleteWithdrawPermanent(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("restoreAllWithdraw delegates and returns success")
  void restoreAllWithdraw(VertxTestContext ctx) {
    when(service.restoreAllWithdraws()).thenReturn(Future.succeededFuture());

    handler.restoreAllWithdraw(Empty.getDefaultInstance())
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deleteAllWithdrawPermanent delegates and returns success")
  void deleteAllWithdrawPermanent(VertxTestContext ctx) {
    when(service.deleteAllPermanentWithdraws()).thenReturn(Future.succeededFuture());

    handler.deleteAllWithdrawPermanent(Empty.getDefaultInstance())
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }
}
