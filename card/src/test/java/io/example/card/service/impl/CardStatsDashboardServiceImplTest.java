package io.example.card.service.impl;

import io.example.card.model.CardStats;
import io.example.card.repository.*;
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

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class CardStatsDashboardServiceImplTest {

  @Mock
  private CardDashboardBalanceRepository balanceRepo;

  @Mock
  private CardDashboardTopupRepository topupRepo;

  @Mock
  private CardDashboardWithdrawRepository withdrawRepo;

  @Mock
  private CardDashboardTransactionRepository transactionRepo;

  @Mock
  private CardDashboardTransferRepository transferRepo;

  @Mock
  private RedisService redis;

  @Mock
  private TracingMetrics metrics;

  private CardStatsDashboardServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new CardStatsDashboardServiceImpl(balanceRepo, topupRepo, withdrawRepo,
        transactionRepo, transferRepo, redis, metrics);
  }

  private void mockTracing() {
    var ctx = new TracingContext(Context.root(), Instant.now());
    lenient().when(metrics.startSpan(anyString())).thenReturn(ctx);
    lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(ctx);
  }

  /* ─── getDashboardCard ─── */

  @Test
  @DisplayName("getDashboardCard returns cached dashboard")
  void getDashboardCardCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = "{\"totalBalance\":1000,\"totalTopup\":200,\"totalWithdraw\":50,\"totalTransaction\":300,\"totalTransfer\":150}";

    when(redis.get("stats:dashboard:global")).thenReturn(Future.succeededFuture(json));

    service.getDashboardCard()
        .onComplete(ctx.succeeding(dash -> ctx.verify(() -> {
          assertThat(dash.getTotalBalance()).isEqualTo(1000L);
          assertThat(dash.getTotalTopup()).isEqualTo(200L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getDashboardCard fans out to 5 repos and caches result")
  void getDashboardCardCacheMiss(VertxTestContext ctx) {
    mockTracing();

    when(redis.get("stats:dashboard:global")).thenReturn(Future.succeededFuture(null));
    when(balanceRepo.getTotalBalances()).thenReturn(Future.succeededFuture(1000L));
    when(topupRepo.getTotalTopAmount()).thenReturn(Future.succeededFuture(200L));
    when(withdrawRepo.getTotalWithdrawAmount()).thenReturn(Future.succeededFuture(50L));
    when(transactionRepo.getTotalTransactionAmount()).thenReturn(Future.succeededFuture(300L));
    when(transferRepo.getTotalTransferAmount()).thenReturn(Future.succeededFuture(150L));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    service.getDashboardCard()
        .onComplete(ctx.succeeding(dash -> ctx.verify(() -> {
          assertThat(dash.getTotalBalance()).isEqualTo(1000L);
          assertThat(dash.getTotalTopup()).isEqualTo(200L);
          assertThat(dash.getTotalWithdraw()).isEqualTo(50L);
          assertThat(dash.getTotalTransaction()).isEqualTo(300L);
          assertThat(dash.getTotalTransfer()).isEqualTo(150L);
          verify(balanceRepo).getTotalBalances();
          verify(topupRepo).getTotalTopAmount();
          verify(withdrawRepo).getTotalWithdrawAmount();
          verify(transactionRepo).getTotalTransactionAmount();
          verify(transferRepo).getTotalTransferAmount();
          ctx.completeNow();
        })));
  }

  /* ─── getDashboardCardByCardNumber ─── */

  @Test
  @DisplayName("getDashboardCardByCardNumber returns cached per-card dashboard")
  void getDashboardCardByCardNumberCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = "{\"totalBalance\":500,\"totalTopup\":100,\"totalWithdraw\":25,"
        + "\"totalTransaction\":150,\"totalTransferSend\":50,\"totalTransferReceiver\":30}";

    when(redis.get("stats:dashboard:card:4111111111111111")).thenReturn(Future.succeededFuture(json));

    service.getDashboardCardByCardNumber("4111111111111111")
        .onComplete(ctx.succeeding(dash -> ctx.verify(() -> {
          assertThat(dash.getTotalBalance()).isEqualTo(500L);
          assertThat(dash.getTotalTransferSend()).isEqualTo(50L);
          assertThat(dash.getTotalTransferReceiver()).isEqualTo(30L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getDashboardCardByCardNumber fans out to 6 repo calls and caches")
  void getDashboardCardByCardNumberCacheMiss(VertxTestContext ctx) {
    mockTracing();

    when(redis.get("stats:dashboard:card:4111111111111111")).thenReturn(Future.succeededFuture(null));
    when(balanceRepo.getTotalBalanceByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(500L));
    when(topupRepo.getTotalTopupAmountByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(100L));
    when(withdrawRepo.getTotalWithdrawAmountByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(25L));
    when(transactionRepo.getTotalTransactionAmountByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(150L));
    when(transferRepo.getTotalTransferAmountBySender("4111111111111111")).thenReturn(Future.succeededFuture(50L));
    when(transferRepo.getTotalTransferAmountByReceiver("4111111111111111")).thenReturn(Future.succeededFuture(30L));
    when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    service.getDashboardCardByCardNumber("4111111111111111")
        .onComplete(ctx.succeeding(dash -> ctx.verify(() -> {
          assertThat(dash.getTotalBalance()).isEqualTo(500L);
          assertThat(dash.getTotalTransferSend()).isEqualTo(50L);
          assertThat(dash.getTotalTransferReceiver()).isEqualTo(30L);
          verify(transferRepo).getTotalTransferAmountBySender("4111111111111111");
          verify(transferRepo).getTotalTransferAmountByReceiver("4111111111111111");
          ctx.completeNow();
        })));
  }
}
