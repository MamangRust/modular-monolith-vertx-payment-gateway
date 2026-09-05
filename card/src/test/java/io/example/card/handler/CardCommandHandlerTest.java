package io.example.card.handler;

import com.google.protobuf.Empty;
import io.example.card.model.Card;
import io.example.card.service.CardCommandService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.card.Card.FindByIdCardRequest;
import pb.card.CardCommand.CreateCardRequest;
import pb.card.CardCommand.UpdateCardRequest;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class CardCommandHandlerTest {

  @Mock
  private CardCommandService service;

  private CardCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new CardCommandHandler(service);
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

  /* ─── createCard ─── */

  @Test
  @DisplayName("createCard delegates and returns response")
  void createCard(VertxTestContext ctx) {
    when(service.createCard(any())).thenReturn(Future.succeededFuture(aCard()));

    var req = CreateCardRequest.newBuilder()
        .setUserId(42).setCardType("CREDIT").setCvv("123").setCardProvider("VISA")
        .build();

    handler.createCard(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getId()).isEqualTo(1);
          assertThat(resp.getData().getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  /* ─── updateCard ─── */

  @Test
  @DisplayName("updateCard delegates and returns response")
  void updateCard(VertxTestContext ctx) {
    when(service.updateCard(any())).thenReturn(Future.succeededFuture(aCard()));

    var req = UpdateCardRequest.newBuilder().setCardId(1).setUserId(42).build();

    handler.updateCard(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── trashedCard ─── */

  @Test
  @DisplayName("trashedCard delegates and returns delete-at response")
  void trashedCard(VertxTestContext ctx) {
    var trashed = aCard();
    trashed.setDeletedAt(Timestamp.from(Instant.parse("2026-06-25T10:00:00Z")));
    when(service.trashedCard(1)).thenReturn(Future.succeededFuture(trashed));

    var req = FindByIdCardRequest.newBuilder().setCardId(1).build();

    handler.trashedCard(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getId()).isEqualTo(1);
          assertThat(resp.getData().hasDeletedAt()).isTrue();
          ctx.completeNow();
        })));
  }

  /* ─── restoreCard ─── */

  @Test
  @DisplayName("restoreCard delegates and returns delete-at response")
  void restoreCard(VertxTestContext ctx) {
    when(service.restoreCard(1)).thenReturn(Future.succeededFuture(aCard()));

    var req = FindByIdCardRequest.newBuilder().setCardId(1).build();

    handler.restoreCard(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── deleteCardPermanent ─── */

  @Test
  @DisplayName("deleteCardPermanent delegates and returns success")
  void deleteCardPermanent(VertxTestContext ctx) {
    when(service.deleteCardPermanent(1)).thenReturn(Future.succeededFuture());

    var req = FindByIdCardRequest.newBuilder().setCardId(1).build();

    handler.deleteCardPermanent(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  /* ─── restoreAllCard ─── */

  @Test
  @DisplayName("restoreAllCard delegates and returns success")
  void restoreAllCard(VertxTestContext ctx) {
    when(service.restoreAllCard()).thenReturn(Future.succeededFuture());

    handler.restoreAllCard(Empty.getDefaultInstance())
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  /* ─── deleteAllCardPermanent ─── */

  @Test
  @DisplayName("deleteAllCardPermanent delegates and returns success")
  void deleteAllCardPermanent(VertxTestContext ctx) {
    when(service.deleteAllCardPermanent()).thenReturn(Future.succeededFuture());

    handler.deleteAllCardPermanent(Empty.getDefaultInstance())
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  /* ─── error path ─── */

  @Test
  @DisplayName("createCard delegates error when service fails")
  void createCardError(VertxTestContext ctx) {
    when(service.createCard(any()))
        .thenReturn(Future.failedFuture(new RuntimeException("DB error")));

    var req = CreateCardRequest.newBuilder().setUserId(1).build();

    handler.createCard(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(io.grpc.StatusRuntimeException.class);
          ctx.completeNow();
        })));
  }
}
