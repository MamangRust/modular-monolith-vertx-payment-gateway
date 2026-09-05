package io.example.saldo.service.impl;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.example.saldo.domain.requests.CreateSaldoRequest;
import io.example.saldo.domain.requests.UpdateSaldoBalanceRequest;
import io.example.saldo.domain.requests.UpdateSaldoRequest;
import io.example.saldo.domain.requests.UpdateSaldoWithdrawRequest;
import io.example.saldo.model.Saldo;
import io.example.saldo.repository.CardClientRepository;
import io.example.saldo.repository.SaldoCommandRepository;
import io.example.saldo.repository.SaldoQueryRepository;
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
import pb.card.Card.CardWithEmailResponse;

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
class SaldoCommandServiceImplTest {

  @Mock
  private SaldoCommandRepository repo;

  @Mock
  private SaldoQueryRepository queryRepository;

  @Mock
  private CardClientRepository repoCard;

  @Mock
  private RedisService redisService;

  @Mock
  private KafkaService kafkaService;

  @Mock
  private TracingMetrics tracingMetrics;

  private SaldoCommandServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new SaldoCommandServiceImpl(repo, queryRepository, repoCard, redisService, kafkaService, tracingMetrics);
  }

  private void mockTracing() {
    var ctx = new TracingContext(Context.root(), Instant.now());
    lenient().when(tracingMetrics.startSpan(anyString())).thenReturn(ctx);
    lenient().when(tracingMetrics.startSpan(anyString(), any())).thenReturn(ctx);
  }

  private Timestamp now() {
    return Timestamp.from(Instant.parse("2026-06-26T10:00:00Z"));
  }

  private Saldo aSaldo() {
    return Saldo.builder()
        .id(1)
        .cardNumber("4111111111111111")
        .totalBalance(1_000_000L)
        .createdAt(now())
        .updatedAt(now())
        .build();
  }

  private CardWithEmailResponse aCardEmail() {
    return CardWithEmailResponse.newBuilder()
        .setId(1)
        .setUserId(42)
        .setEmail("alice@example.com")
        .setCardNumber("4111111111111111")
        .build();
  }

  /* ─── createSaldo ─── */

  @Test
  @DisplayName("createSaldo creates saldo and sends email")
  void createSaldoSuccess(VertxTestContext ctx) {
    mockTracing();
    var saldo = aSaldo();

    when(repoCard.findUserCardByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(aCardEmail()));
    when(repo.createSaldo(any(CreateSaldoRequest.class))).thenReturn(Future.succeededFuture(saldo));
    when(redisService.deleteByPattern(anyString())).thenReturn(Future.succeededFuture(1L));
    when(kafkaService.sendMessage(anyString(), anyString(), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture());

    service.createSaldo(CreateSaldoRequest.builder().cardNumber("4111111111111111").totalBalance(1_000_000L).build())
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
          verify(kafkaService).sendMessage(eq("email-service-topic-saldo-create"), eq("1"), any());
          ctx.completeNow();
        })));
  }

  /* ─── updateSaldo ─── */

  @Test
  @DisplayName("updateSaldo updates saldo and evicts cache")
  void updateSaldoSuccess(VertxTestContext ctx) {
    mockTracing();
    var saldo = aSaldo();
    var req = UpdateSaldoRequest.builder().saldoId(1).cardNumber("4111111111111111").totalBalance(2_000_000L).build();

    when(repoCard.getCardByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(
        pb.card.Card.ApiResponseCard.newBuilder().setStatus("success").build()));
    when(repo.updateSaldo(req)).thenReturn(Future.succeededFuture(saldo));
    when(redisService.delete("saldo:1")).thenReturn(Future.succeededFuture(1L));
    when(redisService.deleteByPattern(anyString())).thenReturn(Future.succeededFuture(1L));

    service.updateSaldo(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          verify(redisService).delete("saldo:1");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateSaldo fails when saldo not found")
  void updateSaldoNotFound(VertxTestContext ctx) {
    mockTracing();

    when(repoCard.getCardByCardNumber(anyString())).thenReturn(Future.succeededFuture(
        pb.card.Card.ApiResponseCard.newBuilder().setStatus("success").build()));
    when(repo.updateSaldo(any())).thenReturn(Future.succeededFuture(null));

    service.updateSaldo(UpdateSaldoRequest.builder().saldoId(99).cardNumber("4111111111111111").totalBalance(1_000_000L).build())
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }

  /* ─── updateSaldoBalance ─── */

  @Test
  @DisplayName("updateSaldoBalance updates balance and evicts cache")
  void updateSaldoBalanceSuccess(VertxTestContext ctx) {
    mockTracing();
    var saldo = aSaldo();
    var req = UpdateSaldoBalanceRequest.builder().cardNumber("4111111111111111").totalBalance(500_000L).build();

    when(repo.updateSaldoBalance(req)).thenReturn(Future.succeededFuture(saldo));
    when(redisService.delete("saldo:1")).thenReturn(Future.succeededFuture(1L));
    when(redisService.deleteByPattern(anyString())).thenReturn(Future.succeededFuture(1L));

    service.updateSaldoBalance(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          verify(redisService).delete("saldo:1");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateSaldoBalance fails when saldo not found")
  void updateSaldoBalanceNotFound(VertxTestContext ctx) {
    mockTracing();

    when(repo.updateSaldoBalance(any())).thenReturn(Future.succeededFuture(null));

    service.updateSaldoBalance(UpdateSaldoBalanceRequest.builder().cardNumber("0000000000000000").totalBalance(500_000L).build())
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }

  /* ─── updateSaldoWithdraw ─── */

  @Test
  @DisplayName("updateSaldoWithdraw updates withdraw and evicts cache")
  void updateSaldoWithdrawSuccess(VertxTestContext ctx) {
    mockTracing();
    var saldo = aSaldo();

    when(repo.updateSaldoWithdraw(any())).thenReturn(Future.succeededFuture(saldo));
    when(redisService.delete("saldo:1")).thenReturn(Future.succeededFuture(1L));
    when(redisService.deleteByPattern(anyString())).thenReturn(Future.succeededFuture(1L));

    service.updateSaldoWithdraw(UpdateSaldoWithdrawRequest.builder()
            .cardNumber("4111111111111111").withdrawAmount(100_000L)
            .withdrawTime(java.time.LocalDateTime.of(2026, 6, 26, 10, 0))
            .build())
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          verify(redisService).delete("saldo:1");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateSaldoWithdraw fails when saldo not found or insufficient balance")
  void updateSaldoWithdrawNotFound(VertxTestContext ctx) {
    mockTracing();

    when(repo.updateSaldoWithdraw(any())).thenReturn(Future.succeededFuture(null));

    service.updateSaldoWithdraw(UpdateSaldoWithdrawRequest.builder()
            .cardNumber("0000000000000000").withdrawAmount(100_000L)
            .withdrawTime(java.time.LocalDateTime.of(2026, 6, 26, 10, 0))
            .build())
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class);
          ctx.completeNow();
        })));
  }

  /* ─── trashSaldo ─── */

  @Test
  @DisplayName("trashSaldo soft-deletes and evicts cache")
  void trashSaldoSuccess(VertxTestContext ctx) {
    mockTracing();
    var saldo = aSaldo();

    when(repo.trash(1)).thenReturn(Future.succeededFuture(saldo));
    when(redisService.delete("saldo:1")).thenReturn(Future.succeededFuture(1L));
    when(redisService.deleteByPattern(anyString())).thenReturn(Future.succeededFuture(1L));

    service.trashSaldo(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          verify(redisService).delete("saldo:1");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("trashSaldo fails when saldo not found")
  void trashSaldoNotFound(VertxTestContext ctx) {
    mockTracing();

    when(repo.trash(99)).thenReturn(Future.succeededFuture(null));

    service.trashSaldo(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }

  /* ─── restoreSaldo ─── */

  @Test
  @DisplayName("restoreSaldo restores trashed saldo and evicts cache")
  void restoreSaldoSuccess(VertxTestContext ctx) {
    mockTracing();
    var saldo = aSaldo();

    when(queryRepository.findByTrashedId(1)).thenReturn(Future.succeededFuture(saldo));
    when(repo.restore(1)).thenReturn(Future.succeededFuture(saldo));
    when(redisService.delete("saldo:1")).thenReturn(Future.succeededFuture(1L));
    when(redisService.deleteByPattern(anyString())).thenReturn(Future.succeededFuture(1L));

    service.restoreSaldo(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          verify(redisService).delete("saldo:1");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("restoreSaldo fails when not trashed")
  void restoreSaldoNotTrashed(VertxTestContext ctx) {
    mockTracing();

    when(queryRepository.findByTrashedId(99)).thenReturn(Future.succeededFuture(null));

    service.restoreSaldo(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class);
          ctx.completeNow();
        })));
  }

  /* ─── deleteSaldoPermanently ─── */

  @Test
  @DisplayName("deleteSaldoPermanently deletes trashed saldo and evicts cache")
  void deleteSaldoPermanentlySuccess(VertxTestContext ctx) {
    mockTracing();
    var saldo = aSaldo();

    when(queryRepository.findByTrashedId(1)).thenReturn(Future.succeededFuture(saldo));
    when(repo.deletePermanent(1)).thenReturn(Future.succeededFuture(true));
    when(redisService.delete("saldo:1")).thenReturn(Future.succeededFuture(1L));
    when(redisService.deleteByPattern(anyString())).thenReturn(Future.succeededFuture(1L));

    service.deleteSaldoPermanently(1)
        .onComplete(ctx.succeeding(v -> ctx.verify(ctx::completeNow)));
  }

  @Test
  @DisplayName("deleteSaldoPermanently fails when not trashed")
  void deleteSaldoPermanentlyNotTrashed(VertxTestContext ctx) {
    mockTracing();

    when(queryRepository.findByTrashedId(99)).thenReturn(Future.succeededFuture(null));

    service.deleteSaldoPermanently(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deleteSaldoPermanently fails when delete returns false")
  void deleteSaldoPermanentlyReturnsFalse(VertxTestContext ctx) {
    mockTracing();
    var saldo = aSaldo();

    when(queryRepository.findByTrashedId(1)).thenReturn(Future.succeededFuture(saldo));
    when(repo.deletePermanent(1)).thenReturn(Future.succeededFuture(false));

    service.deleteSaldoPermanently(1)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class);
          ctx.completeNow();
        })));
  }

  /* ─── restoreAllSaldos ─── */

  @Test
  @DisplayName("restoreAllSaldos restores all and evicts list cache")
  void restoreAllSaldosSuccess(VertxTestContext ctx) {
    mockTracing();

    when(repo.restoreAll()).thenReturn(Future.succeededFuture(3));
    when(redisService.deleteByPattern("saldo:all:*")).thenReturn(Future.succeededFuture(1L));
    when(redisService.deleteByPattern("saldo:active:*")).thenReturn(Future.succeededFuture(1L));
    when(redisService.deleteByPattern("saldo:stats:*")).thenReturn(Future.succeededFuture(1L));

    service.restoreAllSaldos()
        .onComplete(ctx.succeeding(v -> ctx.verify(ctx::completeNow)));
  }

  @Test
  @DisplayName("restoreAllSaldos fails when no trashed saldos found")
  void restoreAllSaldosNotFound(VertxTestContext ctx) {
    mockTracing();

    when(repo.restoreAll()).thenReturn(Future.succeededFuture(0));

    service.restoreAllSaldos()
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }

  /* ─── deleteAllPermanentSaldos ─── */

  @Test
  @DisplayName("deleteAllPermanentSaldos deletes all and evicts list cache")
  void deleteAllPermanentSaldosSuccess(VertxTestContext ctx) {
    mockTracing();

    when(repo.deleteAllPermanent()).thenReturn(Future.succeededFuture(2));
    when(redisService.deleteByPattern("saldo:all:*")).thenReturn(Future.succeededFuture(1L));
    when(redisService.deleteByPattern("saldo:active:*")).thenReturn(Future.succeededFuture(1L));
    when(redisService.deleteByPattern("saldo:stats:*")).thenReturn(Future.succeededFuture(1L));

    service.deleteAllPermanentSaldos()
        .onComplete(ctx.succeeding(v -> ctx.verify(ctx::completeNow)));
  }

  @Test
  @DisplayName("deleteAllPermanentSaldos fails when no trashed saldos found")
  void deleteAllPermanentSaldosNotFound(VertxTestContext ctx) {
    mockTracing();

    when(repo.deleteAllPermanent()).thenReturn(Future.succeededFuture(0));

    service.deleteAllPermanentSaldos()
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }
}
