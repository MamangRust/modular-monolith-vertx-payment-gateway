package io.example.withdraw.service.impl;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.InsufficientBalanceException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.example.withdraw.domain.requests.CreateWithdrawRequest;
import io.example.withdraw.domain.requests.UpdateSaldoBalance;
import io.example.withdraw.domain.requests.UpdateWithdrawRequest;
import io.example.withdraw.domain.requests.UpdateWithdrawStatus;
import io.example.withdraw.model.Withdraw;
import io.example.withdraw.model.WithdrawResponse;
import io.example.withdraw.model.WithdrawResponseDeleteAt;
import io.example.withdraw.repository.CardClientRepository;
import io.example.withdraw.repository.SaldoClientRepository;
import io.example.withdraw.repository.WithdrawCommandRepository;
import io.example.withdraw.repository.WithdrawQueryRepository;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import pb.card.Card.ApiResponseCard;
import pb.card.Card.CardResponse;
import pb.card.Card.CardWithEmailResponse;
import pb.saldo.Saldo.ApiResponseSaldo;
import pb.saldo.Saldo.SaldoResponse;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class WithdrawCommandServiceImplTest {

  @Mock
  private WithdrawCommandRepository repo;

  @Mock
  private WithdrawQueryRepository queryRepo;

  @Mock
  private CardClientRepository repoCard;

  @Mock
  private SaldoClientRepository repoSaldo;

  @Mock
  private RedisService redisService;

  @Mock
  private TracingMetrics tracingMetrics;

  @Mock
  private KafkaService kafkaService;

  private WithdrawCommandServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new WithdrawCommandServiceImpl(repo, queryRepo, repoCard, repoSaldo,
        redisService, kafkaService, tracingMetrics);
  }

  private void mockTracing() {
    var tc = new TracingContext(Context.root(), Instant.now());
    lenient().when(tracingMetrics.startSpan(anyString())).thenReturn(tc);
    lenient().when(tracingMetrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tc);
  }

  private OffsetDateTime now() {
    return OffsetDateTime.of(2026, 6, 26, 10, 0, 0, 0, ZoneOffset.UTC);
  }

  private Withdraw aWithdraw() {
    return Withdraw.builder()
        .id(1)
        .withdrawNo("WDR001")
        .cardNumber("4111111111111111")
        .withdrawAmount(500_000L)
        .status("success")
        .withdrawTime(now())
        .createdAt(now())
        .updatedAt(now())
        .build();
  }

  private void stubCacheDeletes() {
    when(redisService.delete(anyString())).thenReturn(Future.succeededFuture(1L));
  }

  private void stubSaldo() {
    var saldoResp = ApiResponseSaldo.newBuilder()
        .setData(SaldoResponse.newBuilder().setTotalBalance(1_000_000).build())
        .build();
    when(repoSaldo.getSaldoByCardNumber(anyString())).thenReturn(Future.succeededFuture(saldoResp));
    when(repoSaldo.updateSaldoDelta(anyString(), anyInt()))
        .thenReturn(Future.succeededFuture(saldoResp));
    when(queryRepo.getTodaySuccessfulAmount(anyString())).thenReturn(Future.succeededFuture(0L));
    when(repoSaldo.updateSaldoBalance(any(UpdateSaldoBalance.class)))
        .thenReturn(Future.succeededFuture(saldoResp));
  }

  private CardWithEmailResponse aSenderCard() {
    return CardWithEmailResponse.newBuilder()
        .setId(1).setUserId(42).setEmail("user@example.com")
        .setCardNumber("4111111111111111").setCardType("DEBIT")
        .setCvv("123").setExpireDate("2028-12-31T00:00:00Z")
        .setCardProvider("VISA")
        .build();
  }

  /* ─── createWithdraw ─── */

  @Test
  @DisplayName("createWithdraw creates withdrawal, updates balances, marks success, sends email, evicts cache")
  void createWithdrawSuccess(VertxTestContext ctx) {
    mockTracing();
    var withdraw = aWithdraw();
    var req = CreateWithdrawRequest.builder()
        .cardNumber("4111111111111111")
        .withdrawAmount(500_000)
        .withdrawTime(now())
        .idempotencyKey("withdraw-idem-success")
        .build();

    when(repoCard.findUserCardByCardNumber(eq("4111111111111111")))
        .thenReturn(Future.succeededFuture(aSenderCard()));
    when(repo.findByIdempotencyKey(eq("withdraw-idem-success")))
        .thenReturn(Future.succeededFuture(null));
    when(repo.createWithdraw(eq(req), anyLong())).thenReturn(Future.succeededFuture(withdraw));
    when(repo.updateWithdrawStatus(any(UpdateWithdrawStatus.class)))
        .thenReturn(Future.succeededFuture(withdraw));
    when(kafkaService.sendMessage(anyString(), anyString(), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture());
    stubSaldo();
    stubCacheDeletes();

    service.createWithdraw(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
          assertThat(result.getWithdrawAmount()).isEqualTo(500_000);

          verify(repoCard).findUserCardByCardNumber(eq("4111111111111111"));
          verify(repoSaldo).getSaldoByCardNumber(eq("4111111111111111"));
          verify(repoSaldo).updateSaldoDelta(eq("4111111111111111"), eq(-500_000));
          verify(repo).createWithdraw(eq(req), anyLong());
          var statusReq = new UpdateWithdrawStatus(1, "success");
          verify(repo).updateWithdrawStatus(eq(statusReq));
          verify(kafkaService).sendMessage(eq("email-service-topic-withdraw-create"),
              eq("1"), any(JsonObject.class));
          verify(redisService).delete(eq("withdraw:1"));
          verify(redisService).delete(eq("withdraw:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("createWithdraw fails when insufficient balance")
  void createWithdrawInsufficientBalance(VertxTestContext ctx) {
    mockTracing();
    var req = CreateWithdrawRequest.builder()
        .cardNumber("4111111111111111")
        .withdrawAmount(2_000_000)
        .withdrawTime(now())
        .build();

    var saldoResp = ApiResponseSaldo.newBuilder()
        .setData(SaldoResponse.newBuilder().setTotalBalance(1_000_000).build())
        .build();

    when(repoCard.findUserCardByCardNumber(eq("4111111111111111")))
        .thenReturn(Future.succeededFuture(aSenderCard()));
    when(repoSaldo.getSaldoByCardNumber(eq("4111111111111111")))
        .thenReturn(Future.succeededFuture(saldoResp));
    when(queryRepo.getTodaySuccessfulAmount(eq("4111111111111111")))
        .thenReturn(Future.succeededFuture(0L));

    service.createWithdraw(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(InsufficientBalanceException.class);
          ctx.completeNow();
        })));
  }

  /* ─── updateWithdraw ─── */

  @Test
  @DisplayName("updateWithdraw updates withdrawal, sends email, evicts cache")
  void updateWithdrawSuccess(VertxTestContext ctx) {
    mockTracing();
    var existing = aWithdraw();
    var updated = aWithdraw();
    var req = UpdateWithdrawRequest.builder()
        .withdrawId(1)
        .cardNumber("4111111111111111")
        .withdrawAmount(750_000)
        .withdrawTime(now())
        .build();

    when(queryRepo.getWithdrawById(1)).thenReturn(Future.succeededFuture(existing));
    when(repo.updateWithdraw(eq(req), anyLong())).thenReturn(Future.succeededFuture(updated));
    when(repoCard.findUserCardByCardNumber(eq("4111111111111111")))
        .thenReturn(Future.succeededFuture(aSenderCard()));
    when(repo.updateWithdrawStatus(any(UpdateWithdrawStatus.class)))
        .thenReturn(Future.succeededFuture(updated));
    when(kafkaService.sendMessage(anyString(), anyString(), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture());
    stubSaldo();
    stubCacheDeletes();

    service.updateWithdraw(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          assertThat(result.getWithdrawAmount()).isEqualTo(500_000);

          verify(queryRepo).getWithdrawById(1);
          verify(repo).updateWithdraw(eq(req), anyLong());
          verify(repoSaldo).updateSaldoDelta(eq("4111111111111111"), eq(-250_000));
          var statusReq = new UpdateWithdrawStatus(1, "success");
          verify(repo).updateWithdrawStatus(eq(statusReq));
          verify(kafkaService).sendMessage(eq("email-service-topic-withdraw-update"),
              eq("1"), any(JsonObject.class));
          verify(redisService).delete(eq("withdraw:1"));
          verify(redisService).delete(eq("withdraw:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateWithdraw fails when withdrawal not found")
  void updateWithdrawNotFound(VertxTestContext ctx) {
    mockTracing();
    var req = UpdateWithdrawRequest.builder()
        .withdrawId(99)
        .cardNumber("4111111111111111")
        .withdrawAmount(500_000)
        .withdrawTime(now())
        .build();

    when(queryRepo.getWithdrawById(99)).thenReturn(Future.succeededFuture(null));

    service.updateWithdraw(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class)
              .hasMessage("Withdrawal not found with id: 99");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateWithdraw fails when insufficient balance after diff")
  void updateWithdrawInsufficientBalance(VertxTestContext ctx) {
    mockTracing();
    var existing = aWithdraw(); // amount = 500_000
    var req = UpdateWithdrawRequest.builder()
        .withdrawId(1)
        .cardNumber("4111111111111111")
        .withdrawAmount(1_600_000) // diff = 1_100_000, exceeds balance of 1_000_000
        .withdrawTime(now())
        .build();

    var saldoResp = ApiResponseSaldo.newBuilder()
        .setData(SaldoResponse.newBuilder().setTotalBalance(1_000_000).build())
        .build();

    when(queryRepo.getWithdrawById(1)).thenReturn(Future.succeededFuture(existing));
    when(repoSaldo.getSaldoByCardNumber(eq("4111111111111111")))
        .thenReturn(Future.succeededFuture(saldoResp));
    when(repoSaldo.updateSaldoDelta(eq("4111111111111111"), eq(-1_100_000)))
        .thenReturn(Future.failedFuture(new InsufficientBalanceException(1_000_000, 1_100_000)));
    when(repo.updateWithdrawStatus(any(UpdateWithdrawStatus.class))).thenReturn(Future.succeededFuture(aWithdraw()));

    service.updateWithdraw(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(InsufficientBalanceException.class);
          ctx.completeNow();
        })));
  }

  /* ─── trashWithdraw ─── */

  @Test
  @DisplayName("trashWithdraw soft-deletes and evicts cache")
  void trashWithdrawSuccess(VertxTestContext ctx) {
    mockTracing();
    var withdraw = aWithdraw();

    when(repo.trashWithdraw(1)).thenReturn(Future.succeededFuture(withdraw));
    stubCacheDeletes();

    service.trashWithdraw(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          verify(repo).trashWithdraw(1);
          verify(redisService).delete(eq("withdraw:1"));
          verify(redisService).delete(eq("withdraw:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("trashWithdraw fails when withdrawal not found")
  void trashWithdrawNotFound(VertxTestContext ctx) {
    mockTracing();

    when(repo.trashWithdraw(99)).thenReturn(Future.succeededFuture(null));

    service.trashWithdraw(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class)
              .hasMessage("Withdrawal not found with id: 99");
          ctx.completeNow();
        })));
  }

  /* ─── restoreWithdraw ─── */

  @Test
  @DisplayName("restoreWithdraw restores trashed withdrawal and evicts cache")
  void restoreWithdrawSuccess(VertxTestContext ctx) {
    mockTracing();
    var trashed = aWithdraw();
    trashed.setDeletedAt(OffsetDateTime.of(2026, 6, 25, 10, 0, 0, 0, ZoneOffset.UTC));
    var restored = aWithdraw();

    when(queryRepo.findByTrashed(1)).thenReturn(Future.succeededFuture(trashed));
    when(repo.restoreWithdraw(1)).thenReturn(Future.succeededFuture(restored));
    stubCacheDeletes();

    service.restoreWithdraw(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          verify(queryRepo).findByTrashed(1);
          verify(repo).restoreWithdraw(1);
          verify(redisService).delete(eq("withdraw:1"));
          verify(redisService).delete(eq("withdraw:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("restoreWithdraw fails when withdrawal is not trashed")
  void restoreWithdrawNotTrashed(VertxTestContext ctx) {
    mockTracing();

    when(queryRepo.findByTrashed(99)).thenReturn(Future.succeededFuture(null));

    service.restoreWithdraw(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class)
              .hasMessage("Withdrawal not found or must be trashed first");
          ctx.completeNow();
        })));
  }

  /* ─── deleteWithdrawPermanently ─── */

  @Test
  @DisplayName("deleteWithdrawPermanently deletes trashed withdrawal and evicts cache")
  void deleteWithdrawPermanentlySuccess(VertxTestContext ctx) {
    mockTracing();
    var trashed = aWithdraw();
    trashed.setDeletedAt(OffsetDateTime.of(2026, 6, 25, 10, 0, 0, 0, ZoneOffset.UTC));

    when(queryRepo.findByTrashed(1)).thenReturn(Future.succeededFuture(trashed));
    when(repo.deleteWithdrawPermanently(1)).thenReturn(Future.succeededFuture(true));
    stubCacheDeletes();

    service.deleteWithdrawPermanently(1)
        .onComplete(ctx.succeeding(v -> ctx.verify(() -> {
          verify(queryRepo).findByTrashed(1);
          verify(repo).deleteWithdrawPermanently(1);
          verify(redisService).delete(eq("withdraw:1"));
          verify(redisService).delete(eq("withdraw:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deleteWithdrawPermanently fails when withdrawal is not trashed")
  void deleteWithdrawPermanentlyNotTrashed(VertxTestContext ctx) {
    mockTracing();

    when(queryRepo.findByTrashed(99)).thenReturn(Future.succeededFuture(null));

    service.deleteWithdrawPermanently(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class);
          ctx.completeNow();
        })));
  }

  /* ─── restoreAllWithdraws ─── */

  @Test
  @DisplayName("restoreAllWithdraws restores all trashed withdrawals and evicts list cache")
  void restoreAllWithdrawsSuccess(VertxTestContext ctx) {
    mockTracing();

    when(repo.restoreAllWithdraws()).thenReturn(Future.succeededFuture(3));
    when(redisService.delete("withdraw:list:*")).thenReturn(Future.succeededFuture(1L));

    service.restoreAllWithdraws()
        .onComplete(ctx.succeeding(v -> ctx.verify(() -> {
          verify(repo).restoreAllWithdraws();
          verify(redisService).delete(eq("withdraw:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("restoreAllWithdraws fails when no trashed withdrawals found")
  void restoreAllWithdrawsNone(VertxTestContext ctx) {
    mockTracing();

    when(repo.restoreAllWithdraws()).thenReturn(Future.succeededFuture(0));

    service.restoreAllWithdraws()
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class)
              .hasMessage("No trashed withdrawals found");
          ctx.completeNow();
        })));
  }

  /* ─── deleteAllPermanentWithdraws ─── */

  @Test
  @DisplayName("deleteAllPermanentWithdraws deletes all trashed withdrawals and evicts list cache")
  void deleteAllPermanentWithdrawsSuccess(VertxTestContext ctx) {
    mockTracing();

    when(repo.deleteAllPermanentWithdraws()).thenReturn(Future.succeededFuture(2));
    when(redisService.delete("withdraw:list:*")).thenReturn(Future.succeededFuture(1L));

    service.deleteAllPermanentWithdraws()
        .onComplete(ctx.succeeding(v -> ctx.verify(() -> {
          verify(repo).deleteAllPermanentWithdraws();
          verify(redisService).delete(eq("withdraw:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deleteAllPermanentWithdraws fails when no trashed withdrawals found")
  void deleteAllPermanentWithdrawsNone(VertxTestContext ctx) {
    mockTracing();

    when(repo.deleteAllPermanentWithdraws()).thenReturn(Future.succeededFuture(0));

    service.deleteAllPermanentWithdraws()
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class)
              .hasMessage("No trashed withdrawals found");
          ctx.completeNow();
        })));
  }
}
