package io.example.card.handler;

import io.example.card.model.CardAuthTransaction;
import io.example.card.service.CardAuthorizationService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import pb.card.CardAuthorization;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class CardAuthorizationHandlerTest {

  @Mock
  private CardAuthorizationService service;

  private CardAuthorizationHandler handler;
  private final UUID txnUuid = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    handler = new CardAuthorizationHandler(service);
  }

  private CardAuthTransaction aTxn(String status) {
    return CardAuthTransaction.builder()
        .txnId(txnUuid)
        .cardNumber("4111111111111111")
        .amount(1000L)
        .currency("IDR")
        .status(status)
        .authCode("AUTH123")
        .declineCode("")
        .riskScore(10)
        .build();
  }

  @Test
  @DisplayName("authorize delegates to service and returns AuthorizeResponse")
  void authorizeSuccess(VertxTestContext ctx) {
    var txn = aTxn("APPROVED");
    when(service.authorize(anyString(), any(), anyLong(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Future.succeededFuture(txn));

    var req = CardAuthorization.AuthorizeRequest.newBuilder()
        .setCardNumber("4111111111111111")
        .setMerchantId(123)
        .setAmount(1000L)
        .setCurrency("IDR")
        .setPosEntryMode("01")
        .setMcc("5411")
        .setIdempotencyKey("idem-123")
        .build();

    handler.authorize(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getMessage()).isEqualTo("OK");
          assertThat(resp.getData().getTxnId()).isEqualTo(txnUuid.toString());
          assertThat(resp.getData().getApprovalStatus()).isEqualTo("APPROVED");
          assertThat(resp.getData().getAuthCode()).isEqualTo("AUTH123");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("authorize maps exception when service fails")
  void authorizeError(VertxTestContext ctx) {
    when(service.authorize(anyString(), any(), anyLong(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Future.failedFuture(new RuntimeException("Service Error")));

    var req = CardAuthorization.AuthorizeRequest.getDefaultInstance();

    handler.authorize(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(io.grpc.StatusRuntimeException.class);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("reverse delegates to service and returns ReverseResponse")
  void reverseSuccess(VertxTestContext ctx) {
    var txn = aTxn("REVERSED");
    when(service.reverse(anyString(), anyString(), anyLong(), anyString()))
        .thenReturn(Future.succeededFuture(txn));

    var req = CardAuthorization.ReverseRequest.newBuilder()
        .setTxnId(txnUuid.toString())
        .setCardNumber("4111111111111111")
        .setAmount(1000L)
        .setIdempotencyKey("idem-123")
        .build();

    handler.reverse(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getMessage()).isEqualTo("OK");
          assertThat(resp.getReversed()).isTrue();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("reverse maps exception when service fails")
  void reverseError(VertxTestContext ctx) {
    when(service.reverse(anyString(), anyString(), anyLong(), anyString()))
        .thenReturn(Future.failedFuture(new RuntimeException("Service Error")));

    var req = CardAuthorization.ReverseRequest.getDefaultInstance();

    handler.reverse(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(io.grpc.StatusRuntimeException.class);
          ctx.completeNow();
        })));
  }
}
