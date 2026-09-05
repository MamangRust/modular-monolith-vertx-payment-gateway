package io.example.card.service.impl;

import io.example.card.model.Card;
import io.example.card.repository.CardCommandRepository;
import io.example.card.repository.CardQueryRepository;
import io.example.card.repository.UserClientRepository;
import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.card.CardCommand.CreateCardRequest;
import pb.card.CardCommand.UpdateCardRequest;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class CardCommandServiceImplTest {

  @Mock
  private CardCommandRepository repository;

  @Mock
  private CardQueryRepository queryRepo;

  @Mock
  private UserClientRepository userClient;

  @Mock
  private RedisService redis;

  @Mock
  private TracingMetrics metrics;

  @Mock
  private KafkaService kafkaService;

  private CardCommandServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new CardCommandServiceImpl(repository, queryRepo, userClient, redis, metrics, kafkaService);
  }

  private void mockTracing() {
    var ctx = new TracingContext(Context.root(), Instant.now());
    lenient().when(metrics.startSpan(anyString())).thenReturn(ctx);
    lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(ctx);
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

  private CreateCardRequest aCreateReq() {
    return CreateCardRequest.newBuilder()
        .setUserId(42).setCardType("CREDIT").setCvv("123").setCardProvider("VISA")
        .build();
  }

  /* ─── createCard ─── */

  @Test
  @DisplayName("createCard creates card and sends saldo event")
  void createCardSuccess(VertxTestContext ctx) {
    mockTracing();
    var card = aCard();
    var req = aCreateReq();

    when(userClient.getUserById(42)).thenReturn(Future.succeededFuture(new Object()));
    when(repository.createCard(req)).thenReturn(Future.succeededFuture(card));
    when(kafkaService.sendMessage(anyString(), anyString(), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture());

    service.createCard(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
          verify(kafkaService).sendMessage(eq("saldo-service-topic-create-saldo"), eq("1"), any());
          ctx.completeNow();
        })));
  }

  /* ─── updateCard ─── */

  @Test
  @DisplayName("updateCard updates card and evicts cache")
  void updateCardSuccess(VertxTestContext ctx) {
    mockTracing();
    var card = aCard();
    var req = UpdateCardRequest.newBuilder().setCardId(1).setUserId(42).setCardType("DEBIT").build();

    when(userClient.getUserById(42)).thenReturn(Future.succeededFuture(new Object()));
    when(repository.updateCard(req)).thenReturn(Future.succeededFuture(card));
    when(redis.delete("card:1")).thenReturn(Future.succeededFuture(1L));

    service.updateCard(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          verify(redis).delete("card:1");
          ctx.completeNow();
        })));
  }

  /* ─── trashedCard ─── */

  @Test
  @DisplayName("trashedCard soft-deletes and evicts cache")
  void trashedCardSuccess(VertxTestContext ctx) {
    mockTracing();
    var card = aCard();

    when(repository.trashedCard(1)).thenReturn(Future.succeededFuture(card));
    when(redis.delete("card:1")).thenReturn(Future.succeededFuture(1L));

    service.trashedCard(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          verify(redis).delete("card:1");
          ctx.completeNow();
        })));
  }

  /* ─── restoreCard ─── */

  @Test
  @DisplayName("restoreCard restores trashed card and evicts cache")
  void restoreCardSuccess(VertxTestContext ctx) {
    mockTracing();
    var trashed = aCard();
    trashed.setDeletedAt(Timestamp.from(Instant.parse("2026-06-25T10:00:00Z")));
    var restored = aCard();

    when(queryRepo.findByTrashId(1)).thenReturn(Future.succeededFuture(trashed));
    when(repository.restoreCard(1)).thenReturn(Future.succeededFuture(restored));
    when(redis.delete("card:1")).thenReturn(Future.succeededFuture(1L));

    service.restoreCard(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("restoreCard fails when card is not trashed")
  void restoreCardNotTrashed(VertxTestContext ctx) {
    mockTracing();

    when(queryRepo.findByTrashId(99)).thenReturn(Future.succeededFuture(null));

    service.restoreCard(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class)
              .hasMessage("Card not found or must be trashed first");
          ctx.completeNow();
        })));
  }

  /* ─── deleteCardPermanent ─── */

  @Test
  @DisplayName("deleteCardPermanent deletes trashed card and evicts cache")
  void deleteCardPermanentSuccess(VertxTestContext ctx) {
    mockTracing();
    var trashed = aCard();
    trashed.setDeletedAt(Timestamp.from(Instant.parse("2026-06-25T10:00:00Z")));

    when(queryRepo.findByTrashId(1)).thenReturn(Future.succeededFuture(trashed));
    when(repository.deleteCardPermanent(1)).thenReturn(Future.succeededFuture(true));
    when(redis.delete("card:1")).thenReturn(Future.succeededFuture(1L));

    service.deleteCardPermanent(1)
        .onComplete(ctx.succeeding(v -> ctx.verify(ctx::completeNow)));
  }

  @Test
  @DisplayName("deleteCardPermanent fails when card is not trashed")
  void deleteCardPermanentNotTrashed(VertxTestContext ctx) {
    mockTracing();

    when(queryRepo.findByTrashId(99)).thenReturn(Future.succeededFuture(null));

    service.deleteCardPermanent(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class);
          ctx.completeNow();
        })));
  }

  /* ─── restoreAllCard ─── */

  @Test
  @DisplayName("restoreAllCard restores all trashed cards")
  void restoreAllCardSuccess(VertxTestContext ctx) {
    mockTracing();

    when(repository.restoreAllCards()).thenReturn(Future.succeededFuture(3));

    service.restoreAllCard()
        .onComplete(ctx.succeeding(v -> ctx.verify(ctx::completeNow)));
  }

  @Test
  @DisplayName("restoreAllCard fails when no trashed cards")
  void restoreAllCardNone(VertxTestContext ctx) {
    mockTracing();

    when(repository.restoreAllCards()).thenReturn(Future.succeededFuture(0));

    service.restoreAllCard()
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }

  /* ─── deleteAllCardPermanent ─── */

  @Test
  @DisplayName("deleteAllCardPermanent deletes all trashed cards")
  void deleteAllCardPermanentSuccess(VertxTestContext ctx) {
    mockTracing();

    when(repository.deleteAllCardsPermanent()).thenReturn(Future.succeededFuture(2));

    service.deleteAllCardPermanent()
        .onComplete(ctx.succeeding(v -> ctx.verify(ctx::completeNow)));
  }

  @Test
  @DisplayName("deleteAllCardPermanent fails when no trashed cards")
  void deleteAllCardPermanentNone(VertxTestContext ctx) {
    mockTracing();

    when(repository.deleteAllCardsPermanent()).thenReturn(Future.succeededFuture(0));

    service.deleteAllCardPermanent()
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }
}
