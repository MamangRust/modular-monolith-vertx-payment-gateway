package io.example.card.handler;

import io.example.card.model.Card;
import io.example.card.model.CardEmail;
import io.example.card.service.CardQueryService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.card.Card.FindByCardNumberRequest;
import pb.card.Card.FindByIdCardRequest;
import pb.card.Card.FindByUserIdCardRequest;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class CardQueryHandlerTest {

  @Mock
  private CardQueryService service;

  private CardQueryHandler handler;

  @BeforeEach
  void setUp() {
    handler = new CardQueryHandler(service);
  }

  private Timestamp now() {
    return Timestamp.from(Instant.parse("2026-06-26T10:00:00Z"));
  }

  private Card aCard() {
    return Card.builder()
        .id(1).userId(42).cardNumber("4111111111111111")
        .cardType("CREDIT").expireDate("2028-12-31").cvv("123")
        .cardProvider("VISA").createdAt(now()).updatedAt(now())
        .build();
  }

  /* ─── findByIdCard ─── */

  @Test
  @DisplayName("findByIdCard delegates and returns response")
  void findByIdCard(VertxTestContext ctx) {
    when(service.getCardById(1)).thenReturn(Future.succeededFuture(aCard()));

    var req = FindByIdCardRequest.newBuilder().setCardId(1).build();

    handler.findByIdCard(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getId()).isEqualTo(1);
          assertThat(resp.getData().getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  /* ─── findByUserIdCard ─── */

  @Test
  @DisplayName("findByUserIdCard delegates and returns response")
  void findByUserIdCard(VertxTestContext ctx) {
    when(service.getCardByUserId(42)).thenReturn(Future.succeededFuture(aCard()));

    var req = FindByUserIdCardRequest.newBuilder().setUserId(42).build();

    handler.findByUserIdCard(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getUserId()).isEqualTo(42);
          ctx.completeNow();
        })));
  }

  /* ─── findByCardNumber ─── */

  @Test
  @DisplayName("findByCardNumber delegates and returns response")
  void findByCardNumber(VertxTestContext ctx) {
    when(service.getCardByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(aCard()));

    var req = FindByCardNumberRequest.newBuilder().setCardNumber("4111111111111111").build();

    handler.findByCardNumber(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  /* ─── findUserCardByCardNumber ─── */

  @Test
  @DisplayName("findUserCardByCardNumber delegates and returns card with email")
  void findUserCardByCardNumber(VertxTestContext ctx) {
    var ce = CardEmail.builder()
        .id(1).email("alice@example.com").userId(42)
        .cardNumber("4111111111111111").cardType("CREDIT")
        .expireDate("2028-12-31").cvv("123").cardProvider("VISA")
        .createdAt(now()).updatedAt(now())
        .build();

    when(service.getCardEmailByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(ce));

    var req = FindByCardNumberRequest.newBuilder().setCardNumber("4111111111111111").build();

    handler.findUserCardByCardNumber(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getId()).isEqualTo(1);
          assertThat(resp.getEmail()).isEqualTo("alice@example.com");
          assertThat(resp.getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  /* ─── error path ─── */

  @Test
  @DisplayName("findByIdCard delegates error when service fails")
  void findByIdCardError(VertxTestContext ctx) {
    when(service.getCardById(99))
        .thenReturn(Future.failedFuture(new RuntimeException("Card not found")));

    var req = FindByIdCardRequest.newBuilder().setCardId(99).build();

    handler.findByIdCard(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(io.grpc.StatusRuntimeException.class);
          ctx.completeNow();
        })));
  }
}
