package io.example.card.service.impl;

import io.example.card.model.Card;
import io.example.card.model.CardEmail;
import io.example.card.repository.CardQueryRepository;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class CardQueryServiceImplTest {

  @Mock
  private CardQueryRepository repository;

  @Mock
  private RedisService redis;

  @Mock
  private TracingMetrics metrics;

  private CardQueryServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new CardQueryServiceImpl(repository, redis, metrics);
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

  /* ─── getCardById ─── */

  @Test
  @DisplayName("getCardById returns cached card when available")
  void getCardByIdCacheHit(VertxTestContext ctx) {
    mockTracing();
    // Jackson expects camelCase keys; Card.toJson() uses snake_case,
    // so we provide a manually crafted camelCase JSON string.
    var json = "{\"id\":1,\"userId\":42,\"cardNumber\":\"4111111111111111\","
        + "\"cardType\":\"CREDIT\",\"expireDate\":\"2028-12-31\",\"cvv\":\"123\","
        + "\"cardProvider\":\"VISA\","
        + "\"createdAt\":\"2026-06-26T10:00:00Z\",\"updatedAt\":\"2026-06-26T10:00:00Z\"}";

    when(redis.get("card:id:1")).thenReturn(Future.succeededFuture(json));
    // No repository stub needed — cache hit path doesn't touch DB

    service.getCardById(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getCardById fetches from DB and caches result on cache miss")
  void getCardByIdCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var card = aCard();

    when(redis.get("card:id:1")).thenReturn(Future.succeededFuture(null));
    when(repository.findById(1)).thenReturn(Future.succeededFuture(card));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    service.getCardById(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getId()).isEqualTo(1);
          verify(repository).findById(1);
          verify(redis).setJson("card:id:1", card, Duration.ofMinutes(10));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getCardById fails when card not found in DB")
  void getCardByIdNotFound(VertxTestContext ctx) {
    mockTracing();

    when(redis.get("card:id:99")).thenReturn(Future.succeededFuture(null));
    when(repository.findById(99)).thenReturn(Future.succeededFuture(null));

    service.getCardById(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class)
              .hasMessage("Card not found");
          ctx.completeNow();
        })));
  }

  /* ─── getCardByUserId ─── */

  @Test
  @DisplayName("getCardByUserId returns cached card by userId")
  void getCardByUserIdCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = "{\"id\":1,\"userId\":42,\"cardNumber\":\"4111111111111111\"}";

    when(redis.get("card:user:42")).thenReturn(Future.succeededFuture(json));
    // No repository stub needed — cache hit path doesn't touch DB

    service.getCardByUserId(42)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getUserId()).isEqualTo(42);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getCardByUserId fetches from DB when cache misses")
  void getCardByUserIdCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var card = aCard();

    when(redis.get("card:user:42")).thenReturn(Future.succeededFuture(null));
    when(repository.findByUserId(42)).thenReturn(Future.succeededFuture(card));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    service.getCardByUserId(42)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getUserId()).isEqualTo(42);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getCardByUserId fails when card not found")
  void getCardByUserIdNotFound(VertxTestContext ctx) {
    mockTracing();

    when(redis.get("card:user:99")).thenReturn(Future.succeededFuture(null));
    when(repository.findByUserId(99)).thenReturn(Future.succeededFuture(null));

    service.getCardByUserId(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }

  /* ─── getCardByCardNumber ─── */

  @Test
  @DisplayName("getCardByCardNumber returns cached card by card number")
  void getCardByCardNumberCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = "{\"id\":1,\"userId\":42,\"cardNumber\":\"4111111111111111\"}";

    when(redis.get("card:number:4111111111111111")).thenReturn(Future.succeededFuture(json));
    // No repository stub needed — cache hit path doesn't touch DB

    service.getCardByCardNumber("4111111111111111")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getCardByCardNumber fetches from DB when cache misses")
  void getCardByCardNumberCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var card = aCard();

    when(redis.get("card:number:4111111111111111")).thenReturn(Future.succeededFuture(null));
    when(repository.findByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(card));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    service.getCardByCardNumber("4111111111111111")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  /* ─── getCardEmailByCardNumber ─── */

  @Test
  @DisplayName("getCardEmailByCardNumber returns card with email")
  void getCardEmailByCardNumberSuccess(VertxTestContext ctx) {
    var ce = CardEmail.builder()
        .id(1).email("alice@example.com").userId(42)
        .cardNumber("4111111111111111").cardType("CREDIT")
        .expireDate("2028-12-31").cvv("123").cardProvider("VISA")
        .createdAt(now()).updatedAt(now())
        .build();

    when(repository.getCardEmailByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(ce));

    service.getCardEmailByCardNumber("4111111111111111")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getEmail()).isEqualTo("alice@example.com");
          assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getCardEmailByCardNumber fails when not found")
  void getCardEmailByCardNumberNotFound(VertxTestContext ctx) {
    when(repository.getCardEmailByCardNumber("0000000000000000")).thenReturn(Future.succeededFuture(null));

    service.getCardEmailByCardNumber("0000000000000000")
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }
}
