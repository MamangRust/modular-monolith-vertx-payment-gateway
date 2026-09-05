package io.example.topup.service.impl;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.ConflictException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.example.topup.domain.requests.topup.CreateTopupRequest;
import io.example.topup.domain.requests.topup.UpdateTopupRequest;
import io.example.topup.model.Topup;
import io.example.topup.model.TopupResponse;
import io.example.topup.model.TopupResponseDeleteAt;
import io.example.topup.repository.CardClientRepository;
import io.example.topup.repository.SaldoClientRepository;
import io.example.topup.repository.TopupCommandRepository;
import io.example.topup.repository.TopupQueryRepository;
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

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.argThat;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class TopupCommandServiceImplTest {

  @Mock
  private TopupCommandRepository repo;

  @Mock
  private TopupQueryRepository repoQuery;

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

  private TopupCommandServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new TopupCommandServiceImpl(repo, repoQuery, repoCard, repoSaldo, redisService, tracingMetrics, kafkaService);
  }

  private void mockTracing() {
    var tc = new TracingContext(Context.root(), Instant.now());
    lenient().when(tracingMetrics.startSpan(anyString())).thenReturn(tc);
    lenient().when(tracingMetrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tc);
  }

  private Timestamp now() {
    return Timestamp.from(Instant.parse("2026-06-26T10:00:00Z"));
  }

  private Topup aTopup() {
    return Topup.builder().id(1).cardNumber("4111111111111111").topupNo("TXN001")
        .topupAmount(500_000L).topupMethod("BANK").status("success").topupTime(now())
        .createdAt(now()).updatedAt(now()).build();
  }

  private void stubCacheDeletes() {
    when(redisService.delete(anyString())).thenReturn(Future.succeededFuture(1L));
  }

  private void stubSaldo() {
    var saldoResp = ApiResponseSaldo.newBuilder()
        .setData(SaldoResponse.newBuilder().setTotalBalance(1000000).build())
        .build();
    // createTopup/updateTopup only apply atomic deltas — no read-modify-write.
    when(repoSaldo.updateSaldoDelta(anyString(), anyInt())).thenReturn(Future.succeededFuture(saldoResp));
  }

  /* ─── createTopup ─── */

  @Test
  @DisplayName("createTopup creates topup, updates saldo, marks success, sends email, evicts list cache")
  void createTopupSuccess(VertxTestContext ctx) {
    mockTracing();
    var topup = aTopup();
    var req = CreateTopupRequest.builder()
        .cardNumber("4111111111111111").topupNo("TXN001")
        .topupAmount(500_000).topupMethod("BANK").build();

    var cardEmail = CardWithEmailResponse.newBuilder()
        .setId(1).setUserId(42).setEmail("test@example.com")
        .setCardNumber("4111111111111111").setCardType("CREDIT")
        .setCvv("123").setExpireDate("2028-12-31T00:00:00Z")
        .setCardProvider("VISA")
        .build();

    when(repoCard.getCardEmailByCardNumber(anyString())).thenReturn(Future.succeededFuture(cardEmail));
    when(repo.createTopup(any(CreateTopupRequest.class))).thenReturn(Future.succeededFuture(topup));
    stubSaldo();
    when(repoCard.updateCard(any())).thenReturn(Future.succeededFuture(
        ApiResponseCard.newBuilder().setStatus("success").build()));
    when(repo.updateTopupStatus(any())).thenReturn(Future.succeededFuture(topup));
    when(kafkaService.sendMessage(anyString(), anyString(), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture());
    stubCacheDeletes();

    service.createTopup(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
          assertThat(result.getAmount()).isEqualTo(500_000);

          verify(repoCard).getCardEmailByCardNumber(eq("4111111111111111"));
          verify(repo).createTopup(any(CreateTopupRequest.class));
          // Atomic credit: only the topup amount is applied as a delta.
          verify(repoSaldo).updateSaldoDelta(eq("4111111111111111"), eq(500_000));
          verify(repoCard).updateCard(any());
          verify(repo).updateTopupStatus(any());
          verify(kafkaService).sendMessage(eq("email-service-topic-topup-create"), eq("1"), any(JsonObject.class));
          verify(redisService).delete(eq("topup:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("createTopup compensates saldo and marks failed when card update fails")
  void createTopupCompensatesWhenCardUpdateFails(VertxTestContext ctx) {
    mockTracing();
    var topup = aTopup();
    var req = CreateTopupRequest.builder()
        .cardNumber("4111111111111111").topupNo("TXN001")
        .topupAmount(500_000).topupMethod("BANK").build();
    var cardEmail = CardWithEmailResponse.newBuilder()
        .setId(1).setUserId(42).setEmail("test@example.com")
        .setCardNumber("4111111111111111").setCardType("CREDIT")
        .setCvv("123").setExpireDate("2028-12-31T00:00:00Z")
        .setCardProvider("VISA").build();
    var cardFailure = new RuntimeException("card update unavailable");

    when(repoCard.getCardEmailByCardNumber(eq("4111111111111111")))
        .thenReturn(Future.succeededFuture(cardEmail));
    when(repo.createTopup(any(CreateTopupRequest.class))).thenReturn(Future.succeededFuture(topup));
    when(repoSaldo.updateSaldoDelta(eq("4111111111111111"), eq(500_000)))
        .thenReturn(Future.succeededFuture(ApiResponseSaldo.getDefaultInstance()));
    when(repoSaldo.updateSaldoDelta(eq("4111111111111111"), eq(-500_000)))
        .thenReturn(Future.succeededFuture(ApiResponseSaldo.getDefaultInstance()));
    when(repoCard.updateCard(any())).thenReturn(Future.failedFuture(cardFailure));
    when(repo.updateTopupStatus(any())).thenReturn(Future.succeededFuture(topup));

    service.createTopup(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isSameAs(cardFailure);
          verify(repoSaldo).updateSaldoDelta(eq("4111111111111111"), eq(500_000));
          verify(repoSaldo).updateSaldoDelta(eq("4111111111111111"), eq(-500_000));
          verify(repo).updateTopupStatus(any());
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("createTopup replays only a matching successful idempotency result")
  void createTopupIdempotentReplay(VertxTestContext ctx) {
    mockTracing();
    var existing = aTopup();
    var req = CreateTopupRequest.builder()
        .cardNumber("4111111111111111").topupNo("TXN001")
        .topupAmount(500_000).topupMethod("BANK")
        .idempotencyKey("topup-replay")
        .build();

    when(repo.findByIdempotencyKey(eq("topup-replay"))).thenReturn(Future.succeededFuture(existing));
    stubCacheDeletes();

    service.createTopup(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(existing.getId());
          assertThat(result.getStatus()).isEqualTo("success");
          verify(repo).findByIdempotencyKey(eq("topup-replay"));
          verify(redisService).delete(eq("topup:1"));
          verify(repoCard, org.mockito.Mockito.never()).getCardEmailByCardNumber(anyString());
          verify(repoSaldo, org.mockito.Mockito.never()).updateSaldoDelta(anyString(), anyInt());
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("createTopup rejects an idempotency key with pending or mismatched request")
  void createTopupIdempotencyConflict(VertxTestContext ctx) {
    mockTracing();
    var existing = aTopup();
    existing.setStatus("pending");
    var req = CreateTopupRequest.builder()
        .cardNumber("4111111111111111").topupNo("TXN001")
        .topupAmount(500_000).topupMethod("BANK")
        .idempotencyKey("topup-pending")
        .build();

    when(repo.findByIdempotencyKey(eq("topup-pending"))).thenReturn(Future.succeededFuture(existing));

    service.createTopup(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(ConflictException.class)
              .hasMessageContaining("not replayable");
          verify(repoCard, org.mockito.Mockito.never()).getCardEmailByCardNumber(anyString());
          verify(repoSaldo, org.mockito.Mockito.never()).updateSaldoDelta(anyString(), anyInt());
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("createTopup rejects a matching key when immutable topup number differs")
  void createTopupIdempotencyTopupNoMismatch(VertxTestContext ctx) {
    mockTracing();
    var existing = aTopup();
    var req = CreateTopupRequest.builder()
        .cardNumber("4111111111111111").topupNo("TXN002")
        .topupAmount(500_000).topupMethod("BANK")
        .idempotencyKey("topup-topup-no-mismatch")
        .build();

    when(repo.findByIdempotencyKey(eq("topup-topup-no-mismatch")))
        .thenReturn(Future.succeededFuture(existing));

    service.createTopup(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(ConflictException.class)
              .hasMessageContaining("different topup");
          verify(repoCard, org.mockito.Mockito.never()).getCardEmailByCardNumber(anyString());
          verify(repoSaldo, org.mockito.Mockito.never()).updateSaldoDelta(anyString(), anyInt());
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("createTopup re-reads a concurrent insert and replays its success")
  void createTopupConcurrentInsertReplay(VertxTestContext ctx) {
    mockTracing();
    var existing = aTopup();
    var req = CreateTopupRequest.builder()
        .cardNumber("4111111111111111").topupNo("TXN001")
        .topupAmount(500_000).topupMethod("BANK")
        .idempotencyKey("topup-race")
        .build();
    var cardEmail = CardWithEmailResponse.newBuilder()
        .setId(1).setUserId(42).setEmail("test@example.com")
        .setCardNumber("4111111111111111").setCardType("CREDIT")
        .setCvv("123").setExpireDate("2028-12-31T00:00:00Z")
        .setCardProvider("VISA").build();

    when(repo.findByIdempotencyKey(eq("topup-race")))
        .thenReturn(Future.succeededFuture(null), Future.succeededFuture(existing));
    when(repoCard.getCardEmailByCardNumber(eq("4111111111111111")))
        .thenReturn(Future.succeededFuture(cardEmail));
    when(repo.createTopup(any(CreateTopupRequest.class))).thenReturn(Future.succeededFuture(null));
    stubCacheDeletes();

    service.createTopup(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(existing.getId());
          verify(repo, times(2)).findByIdempotencyKey(eq("topup-race"));
          verify(repoCard).getCardEmailByCardNumber(eq("4111111111111111"));
          verify(repoSaldo, org.mockito.Mockito.never()).updateSaldoDelta(anyString(), anyInt());
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("createTopup records compensation_required when inverse saldo fails")
  void createTopupCompensationRequiredWhenInverseFails(VertxTestContext ctx) {
    mockTracing();
    var topup = aTopup();
    var req = CreateTopupRequest.builder()
        .cardNumber("4111111111111111").topupNo("TXN001")
        .topupAmount(500_000).topupMethod("BANK").build();
    var cardEmail = CardWithEmailResponse.newBuilder()
        .setId(1).setUserId(42).setEmail("test@example.com")
        .setCardNumber("4111111111111111").setCardType("CREDIT")
        .setCvv("123").setExpireDate("2028-12-31T00:00:00Z")
        .setCardProvider("VISA").build();
    var cardFailure = new RuntimeException("card update unavailable");
    var compensationFailure = new RuntimeException("saldo compensation unavailable");

    when(repoCard.getCardEmailByCardNumber(anyString())).thenReturn(Future.succeededFuture(cardEmail));
    when(repo.createTopup(any(CreateTopupRequest.class))).thenReturn(Future.succeededFuture(topup));
    when(repoSaldo.updateSaldoDelta(eq("4111111111111111"), eq(500_000)))
        .thenReturn(Future.succeededFuture(ApiResponseSaldo.getDefaultInstance()));
    when(repoSaldo.updateSaldoDelta(eq("4111111111111111"), eq(-500_000)))
        .thenReturn(Future.failedFuture(compensationFailure));
    when(repoCard.updateCard(any())).thenReturn(Future.failedFuture(cardFailure));
    when(repo.updateTopupStatus(any())).thenReturn(Future.succeededFuture(topup));

    service.createTopup(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).hasMessageContaining("compensation");
          verify(repo).updateTopupStatus(argThat(status -> "compensation_required".equals(status.getStatus())));
          ctx.completeNow();
        })));
  }

  /* ─── updateTopup ─── */

  @Test
  @DisplayName("updateTopup updates topup, adjusts saldo, marks success, evicts cache")
  void updateTopupSuccess(VertxTestContext ctx) {
    mockTracing();
    var existing = aTopup();
    var updated = Topup.builder().id(1).cardNumber("4111111111111111").topupNo("TXN001")
        .topupAmount(750_000L).topupMethod("BANK").topupTime(now())
        .createdAt(now()).updatedAt(now()).build();

    var req = UpdateTopupRequest.builder()
        .topupId(1).cardNumber("4111111111111111")
        .topupAmount(750_000).topupMethod("BANK").build();

    var cardResp = ApiResponseCard.newBuilder()
        .setStatus("success")
        .setData(CardResponse.newBuilder().setId(1).build())
        .build();

    when(repoCard.getCardByCardNumber(anyString())).thenReturn(Future.succeededFuture(cardResp));
    // getTopupById called twice: first for existing context, second after status update
    when(repoQuery.getTopupById(1))
        .thenReturn(Future.succeededFuture(existing), Future.succeededFuture(updated));
    when(repo.updateTopup(any(UpdateTopupRequest.class))).thenReturn(Future.succeededFuture(updated));
    stubSaldo();
    when(repo.updateTopupStatus(any())).thenReturn(Future.succeededFuture(updated));
    stubCacheDeletes();

    service.updateTopup(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          assertThat(result.getAmount()).isEqualTo(750_000);

          verify(repoCard).getCardByCardNumber(eq("4111111111111111"));
          verify(repoQuery, times(2)).getTopupById(1);
          verify(repo).updateTopup(any(UpdateTopupRequest.class));
          // Atomic adjustment by difference (750k - 500k = +250k delta).
          verify(repoSaldo).updateSaldoDelta(eq("4111111111111111"), eq(250_000));
          verify(repo).updateTopupStatus(any());
          verify(redisService).delete(eq("topup:1"));
          verify(redisService).delete(eq("topup:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateTopup fails when existing topup not found")
  void updateTopupNotFound(VertxTestContext ctx) {
    mockTracing();
    var req = UpdateTopupRequest.builder()
        .topupId(99).cardNumber("4111111111111111")
        .topupAmount(500_000).topupMethod("BANK").build();

    var cardResp = ApiResponseCard.newBuilder()
        .setStatus("success")
        .setData(CardResponse.newBuilder().setId(1).build())
        .build();

    when(repoCard.getCardByCardNumber(anyString())).thenReturn(Future.succeededFuture(cardResp));
    when(repoQuery.getTopupById(99)).thenReturn(Future.succeededFuture(null));
    when(repo.updateTopupStatus(any())).thenReturn(Future.succeededFuture(aTopup()));

    service.updateTopup(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          ctx.completeNow();
        })));
  }

  /* ─── trashTopup ─── */

  @Test
  @DisplayName("trashTopup soft-deletes and evicts cache")
  void trashTopupSuccess(VertxTestContext ctx) {
    mockTracing();
    var topup = aTopup();

    when(repo.trashTopup(1)).thenReturn(Future.succeededFuture(topup));
    stubCacheDeletes();

    service.trashTopup(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          verify(repo).trashTopup(1);
          verify(redisService).delete(eq("topup:1"));
          verify(redisService).delete(eq("topup:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("trashTopup fails when topup not found")
  void trashTopupNotFound(VertxTestContext ctx) {
    mockTracing();

    when(repo.trashTopup(99)).thenReturn(Future.succeededFuture(null));

    service.trashTopup(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }

  /* ─── restoreTopup ─── */

  @Test
  @DisplayName("restoreTopup restores trashed topup and evicts cache")
  void restoreTopupSuccess(VertxTestContext ctx) {
    mockTracing();
    var trashed = aTopup();
    trashed.setDeletedAt(Timestamp.from(Instant.parse("2026-06-25T10:00:00Z")));
    var restored = aTopup();

    when(repoQuery.findByTrashed(1)).thenReturn(Future.succeededFuture(trashed));
    when(repo.restoreTopup(1)).thenReturn(Future.succeededFuture(restored));
    stubCacheDeletes();

    service.restoreTopup(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          verify(repoQuery).findByTrashed(1);
          verify(repo).restoreTopup(1);
          verify(redisService).delete(eq("topup:1"));
          verify(redisService).delete(eq("topup:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("restoreTopup fails when topup is not trashed")
  void restoreTopupNotTrashed(VertxTestContext ctx) {
    mockTracing();

    when(repoQuery.findByTrashed(99)).thenReturn(Future.succeededFuture(null));

    service.restoreTopup(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class)
              .hasMessage("Topup not found or must be trashed first");
          ctx.completeNow();
        })));
  }

  /* ─── deleteTopupPermanently ─── */

  @Test
  @DisplayName("deleteTopupPermanently deletes trashed topup and evicts cache")
  void deleteTopupPermanentlySuccess(VertxTestContext ctx) {
    mockTracing();
    var trashed = aTopup();
    trashed.setDeletedAt(Timestamp.from(Instant.parse("2026-06-25T10:00:00Z")));

    when(repoQuery.findByTrashed(1)).thenReturn(Future.succeededFuture(trashed));
    when(repo.deleteTopupPermanently(1)).thenReturn(Future.succeededFuture(true));
    stubCacheDeletes();

    service.deleteTopupPermanently(1)
        .onComplete(ctx.succeeding(v -> ctx.verify(() -> {
          verify(repoQuery).findByTrashed(1);
          verify(repo).deleteTopupPermanently(1);
          verify(redisService).delete(eq("topup:1"));
          verify(redisService).delete(eq("topup:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deleteTopupPermanently fails when topup is not trashed")
  void deleteTopupPermanentlyNotTrashed(VertxTestContext ctx) {
    mockTracing();

    when(repoQuery.findByTrashed(99)).thenReturn(Future.succeededFuture(null));

    service.deleteTopupPermanently(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class);
          ctx.completeNow();
        })));
  }

  /* ─── restoreAllTopups ─── */

  @Test
  @DisplayName("restoreAllTopups restores all trashed topups and evicts list cache")
  void restoreAllTopupsSuccess(VertxTestContext ctx) {
    mockTracing();

    when(repo.restoreAllTopups()).thenReturn(Future.succeededFuture(3));
    stubCacheDeletes();

    service.restoreAllTopups()
        .onComplete(ctx.succeeding(v -> ctx.verify(() -> {
          verify(repo).restoreAllTopups();
          verify(redisService).delete(eq("topup:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("restoreAllTopups fails when no trashed topups found")
  void restoreAllTopupsNone(VertxTestContext ctx) {
    mockTracing();

    when(repo.restoreAllTopups()).thenReturn(Future.succeededFuture(0));

    service.restoreAllTopups()
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class)
              .hasMessage("No trashed topups found");
          ctx.completeNow();
        })));
  }

  /* ─── deleteAllPermanentTopups ─── */

  @Test
  @DisplayName("deleteAllPermanentTopups deletes all trashed topups and evicts list cache")
  void deleteAllPermanentTopupsSuccess(VertxTestContext ctx) {
    mockTracing();

    when(repo.deleteAllPermanentTopups()).thenReturn(Future.succeededFuture(2));
    stubCacheDeletes();

    service.deleteAllPermanentTopups()
        .onComplete(ctx.succeeding(v -> ctx.verify(() -> {
          verify(repo).deleteAllPermanentTopups();
          verify(redisService).delete(eq("topup:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deleteAllPermanentTopups fails when no trashed topups found")
  void deleteAllPermanentTopupsNone(VertxTestContext ctx) {
    mockTracing();

    when(repo.deleteAllPermanentTopups()).thenReturn(Future.succeededFuture(0));

    service.deleteAllPermanentTopups()
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class)
              .hasMessage("No trashed topups found");
          ctx.completeNow();
        })));
  }
}
