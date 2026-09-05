package io.example.card.service.impl;

import io.example.card.model.CardCreditAccount;
import io.example.card.repository.CardCreditAccountRepository;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.KafkaService;
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

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class CreditLimitServiceImplTest {

  @Mock
  private CardCreditAccountRepository creditAccountRepo;

  @Mock
  private RedisService redis;

  @Mock
  private TracingMetrics metrics;

  @Mock
  private KafkaService kafkaService;

  private CreditLimitServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new CreditLimitServiceImpl(creditAccountRepo, redis, metrics, kafkaService);
  }

  private void mockTracing() {
    var ctx = new TracingContext(Context.root(), Instant.now());
    lenient().when(metrics.startSpan(anyString())).thenReturn(ctx);
    lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(ctx);
  }

  private CardCreditAccount aCreditAccount(Long limit) {
    return CardCreditAccount.builder()
        .cardNumber("4111111111111111")
        .creditLimit(limit)
        .usedCredit(2000000L)
        .availableCredit(limit - 2000000L)
        .status("ACTIVE")
        .build();
  }

  @Test
  @DisplayName("getLimit returns cached account on cache hit")
  void getLimitCacheHit(VertxTestContext ctx) {
    mockTracing();
    var account = aCreditAccount(10000000L);
    when(redis.get("card:4111111111111111:credit")).thenReturn(Future.succeededFuture(account.toJson().encode()));

    service.getLimit("4111111111111111")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getCreditLimit()).isEqualTo(10000000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getLimit fetches from DB and sets cache on cache miss")
  void getLimitCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var account = aCreditAccount(10000000L);
    when(redis.get("card:4111111111111111:credit")).thenReturn(Future.succeededFuture(null));
    when(creditAccountRepo.findByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(account));
    when(redis.set(eq("card:4111111111111111:credit"), anyString(), any(Duration.class))).thenReturn(Future.succeededFuture());

    service.getLimit("4111111111111111")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getCreditLimit()).isEqualTo(10000000L);
          verify(redis).set(eq("card:4111111111111111:credit"), anyString(), any(Duration.class));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getLimit fails with NotFoundException if account not in DB")
  void getLimitNotFound(VertxTestContext ctx) {
    mockTracing();
    when(redis.get("card:4111111111111111:credit")).thenReturn(Future.succeededFuture(null));
    when(creditAccountRepo.findByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(null));

    service.getLimit("4111111111111111")
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("setLimit updates existing account and evicts cache")
  void setLimitExisting(VertxTestContext ctx) {
    mockTracing();
    var existing = aCreditAccount(10000000L);
    var updated = aCreditAccount(15000000L);

    when(creditAccountRepo.findByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(existing));
    when(creditAccountRepo.setCreditLimit("4111111111111111", 15000000L)).thenReturn(Future.succeededFuture(updated));
    when(redis.delete("card:4111111111111111:credit")).thenReturn(Future.succeededFuture(1L));
    when(kafkaService.sendMessage(eq("card.limit.changed"), eq("4111111111111111"), any())).thenReturn(Future.succeededFuture());

    service.setLimit("4111111111111111", 15000000L, 15, 1800)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getCreditLimit()).isEqualTo(15000000L);
          verify(redis).delete("card:4111111111111111:credit");
          verify(kafkaService).sendMessage(eq("card.limit.changed"), eq("4111111111111111"), any());
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("setLimit creates new account when not found")
  void setLimitNew(VertxTestContext ctx) {
    mockTracing();
    var created = aCreditAccount(10000000L);

    when(creditAccountRepo.findByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(null));
    when(creditAccountRepo.createAccount("4111111111111111", 10000000L, 15, 1800)).thenReturn(Future.succeededFuture(created));
    when(redis.delete("card:4111111111111111:credit")).thenReturn(Future.succeededFuture(1L));
    when(kafkaService.sendMessage(eq("card.limit.changed"), eq("4111111111111111"), any())).thenReturn(Future.succeededFuture());

    service.setLimit("4111111111111111", 10000000L, 15, 1800)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          verify(creditAccountRepo).createAccount("4111111111111111", 10000000L, 15, 1800);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("adjustLimit modifies limit and evicts cache")
  void adjustLimitSuccess(VertxTestContext ctx) {
    mockTracing();
    var adjusted = aCreditAccount(12000000L);

    when(creditAccountRepo.adjustCreditLimit("4111111111111111", 2000000L)).thenReturn(Future.succeededFuture(adjusted));
    when(redis.delete("card:4111111111111111:credit")).thenReturn(Future.succeededFuture(1L));
    when(kafkaService.sendMessage(eq("card.limit.changed"), eq("4111111111111111"), any())).thenReturn(Future.succeededFuture());

    service.adjustLimit("4111111111111111", 2000000L)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getCreditLimit()).isEqualTo(12000000L);
          verify(redis).delete("card:4111111111111111:credit");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("adjustLimit fails with NotFoundException when account doesn't exist")
  void adjustLimitNotFound(VertxTestContext ctx) {
    mockTracing();
    when(creditAccountRepo.adjustCreditLimit("4111111111111111", 2000000L)).thenReturn(Future.succeededFuture(null));

    service.adjustLimit("4111111111111111", 2000000L)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }
}
