package io.example.transfer.service.impl;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.ConflictException;
import io.example.common.exception.grpc.InsufficientBalanceException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.example.transfer.model.Transfer;
import io.example.transfer.model.TransferResponse;
import io.example.transfer.model.TransferResponseDeleteAt;
import io.example.transfer.repository.CardClientRepository;
import io.example.transfer.repository.SaldoClientRepository;
import io.example.transfer.repository.TransferCommandRepository;
import io.example.transfer.repository.TransferQueryRepository;
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
import pb.transfer.TransferCommand;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class TransferCommandServiceImplTest {

  @Mock
  private TransferCommandRepository repo;

  @Mock
  private TransferQueryRepository queryRepository;

  @Mock
  private CardClientRepository cardClientRepo;

  @Mock
  private SaldoClientRepository saldoClientRepo;

  @Mock
  private RedisService redisService;

  @Mock
  private TracingMetrics tracingMetrics;

  @Mock
  private KafkaService kafkaService;

  private TransferCommandServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new TransferCommandServiceImpl(repo, queryRepository, cardClientRepo, saldoClientRepo,
        redisService, tracingMetrics, kafkaService);
  }

  private void mockTracing() {
    var tc = new TracingContext(Context.root(), Instant.now());
    lenient().when(tracingMetrics.startSpan(anyString())).thenReturn(tc);
    lenient().when(tracingMetrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tc);
  }

  private OffsetDateTime now() {
    return OffsetDateTime.of(2026, 6, 26, 10, 0, 0, 0, ZoneOffset.UTC);
  }

  private Transfer aTransfer() {
    return Transfer.builder().id(1).transferNo("TXN001")
        .transferFrom("4111111111111111").transferTo("5111111111111111")
        .transferAmount(500_000L).status("success")
        .transferTime(now()).createdAt(now()).updatedAt(now()).build();
  }

  private void stubCacheDeletes() {
    when(redisService.delete(anyString())).thenReturn(Future.succeededFuture(1L));
  }

  private void stubSaldo() {
    var saldoResp = ApiResponseSaldo.newBuilder()
        .setData(SaldoResponse.newBuilder().setTotalBalance(1_000_000).build())
        .build();
    when(saldoClientRepo.getSaldoByCardNumber(anyString())).thenReturn(Future.succeededFuture(saldoResp));
    when(saldoClientRepo.updateSaldoDelta(anyString(), anyInt())).thenReturn(Future.succeededFuture(saldoResp));
  }

  /* ─── createTransfer ─── */

  @Test
  @DisplayName("createTransfer creates transfer, updates balances, marks success, sends email, evicts list cache")
  void createTransferSuccess(VertxTestContext ctx) {
    mockTracing();
    var transfer = aTransfer();
    var req = TransferCommand.CreateTransferRequest.newBuilder()
        .setTransferFrom("4111111111111111")
        .setTransferTo("5111111111111111")
        .setTransferAmount(500_000)
        .build();

    var senderCard = CardWithEmailResponse.newBuilder()
        .setId(1).setUserId(42).setEmail("sender@example.com")
        .setCardNumber("4111111111111111").setCardType("DEBIT")
        .setCvv("123").setExpireDate("2028-12-31T00:00:00Z")
        .setCardProvider("VISA")
        .build();
    var receiverCard = ApiResponseCard.newBuilder()
        .setStatus("success")
        .setData(CardResponse.newBuilder().setId(2).build())
        .build();

    when(cardClientRepo.findUserCardByCardNumber(eq("4111111111111111")))
        .thenReturn(Future.succeededFuture(senderCard));
    when(cardClientRepo.getCardByCardNumber(eq("5111111111111111")))
        .thenReturn(Future.succeededFuture(receiverCard));
    when(repo.createTransfer(eq("4111111111111111"), eq("5111111111111111"), eq(500_000L), nullable(String.class)))
        .thenReturn(Future.succeededFuture(transfer));
    stubSaldo();
    when(repo.updateTransferStatus(eq(1), eq("success"))).thenReturn(Future.succeededFuture(transfer));
    when(kafkaService.sendMessage(anyString(), anyString(), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture());
    stubCacheDeletes();

    service.createTransfer(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          assertThat(result.getTransferFrom()).isEqualTo("4111111111111111");
          assertThat(result.getTransferAmount()).isEqualTo(500_000L);

          verify(cardClientRepo).findUserCardByCardNumber(eq("4111111111111111"));
          verify(cardClientRepo).getCardByCardNumber(eq("5111111111111111"));
          verify(repo).createTransfer(eq("4111111111111111"), eq("5111111111111111"), eq(500_000L), nullable(String.class));
          // Atomic legs: sender debited by -amount, receiver credited by +amount.
          verify(saldoClientRepo).updateSaldoDelta(eq("4111111111111111"), eq(-500_000));
          verify(saldoClientRepo).updateSaldoDelta(eq("5111111111111111"), eq(500_000));
          verify(repo).updateTransferStatus(eq(1), eq("success"));
          verify(kafkaService).sendMessage(eq("email-service-topic-transfer-create"), eq("1"), any(JsonObject.class));
          verify(redisService).delete(eq("transfer:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("createTransfer compensates sender when receiver credit fails")
  void createTransferCompensatesWhenReceiverCreditFails(VertxTestContext ctx) {
    mockTracing();
    var transfer = aTransfer();
    var req = TransferCommand.CreateTransferRequest.newBuilder()
        .setTransferFrom("4111111111111111")
        .setTransferTo("5111111111111111")
        .setTransferAmount(500_000)
        .setIdempotencyKey("transfer-idem-123")
        .build();
    var senderCard = CardWithEmailResponse.newBuilder()
        .setId(1).setUserId(42).setEmail("sender@example.com")
        .setCardNumber("4111111111111111").setCardType("DEBIT")
        .setCvv("123").setExpireDate("2028-12-31T00:00:00Z")
        .setCardProvider("VISA").build();
    var receiverCard = ApiResponseCard.newBuilder()
        .setStatus("success").setData(CardResponse.newBuilder().setId(2).build()).build();
    var receiverFailure = new RuntimeException("receiver saldo unavailable");
    var saldoResp = ApiResponseSaldo.newBuilder()
        .setData(SaldoResponse.newBuilder().setTotalBalance(1_000_000).build()).build();

    when(repo.findByIdempotencyKey(eq("transfer-idem-123")))
        .thenReturn(Future.succeededFuture(null));
    when(cardClientRepo.findUserCardByCardNumber(eq("4111111111111111")))
        .thenReturn(Future.succeededFuture(senderCard));
    when(cardClientRepo.getCardByCardNumber(eq("5111111111111111")))
        .thenReturn(Future.succeededFuture(receiverCard));
    when(saldoClientRepo.getSaldoByCardNumber(eq("4111111111111111")))
        .thenReturn(Future.succeededFuture(saldoResp));
    when(repo.createTransfer(eq("4111111111111111"), eq("5111111111111111"), eq(500_000L), eq("transfer-idem-123")))
        .thenReturn(Future.succeededFuture(transfer));
    when(saldoClientRepo.updateSaldoDelta(eq("4111111111111111"), eq(-500_000)))
        .thenReturn(Future.succeededFuture(saldoResp));
    when(saldoClientRepo.updateSaldoDelta(eq("5111111111111111"), eq(500_000)))
        .thenReturn(Future.failedFuture(receiverFailure));
    when(saldoClientRepo.updateSaldoDelta(eq("4111111111111111"), eq(500_000)))
        .thenReturn(Future.succeededFuture(saldoResp));
    when(repo.updateTransferStatus(eq(1), eq("failed"))).thenReturn(Future.succeededFuture(transfer));

    service.createTransfer(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isSameAs(receiverFailure);
          verify(saldoClientRepo).updateSaldoDelta(eq("4111111111111111"), eq(-500_000));
          verify(saldoClientRepo).updateSaldoDelta(eq("5111111111111111"), eq(500_000));
          verify(saldoClientRepo).updateSaldoDelta(eq("4111111111111111"), eq(500_000));
          verify(repo).updateTransferStatus(eq(1), eq("failed"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("createTransfer replays only a matching successful idempotency result")
  void createTransferIdempotentReplay(VertxTestContext ctx) {
    mockTracing();
    var existing = aTransfer();
    var req = TransferCommand.CreateTransferRequest.newBuilder()
        .setTransferFrom("4111111111111111")
        .setTransferTo("5111111111111111")
        .setTransferAmount(500_000)
        .setIdempotencyKey("transfer-replay")
        .build();

    when(repo.findByIdempotencyKey(eq("transfer-replay"))).thenReturn(Future.succeededFuture(existing));
    stubCacheDeletes();

    service.createTransfer(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(existing.getId());
          verify(repo).findByIdempotencyKey(eq("transfer-replay"));
          verify(cardClientRepo, org.mockito.Mockito.never()).findUserCardByCardNumber(anyString());
          verify(saldoClientRepo, org.mockito.Mockito.never()).updateSaldoDelta(anyString(), anyInt());
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("createTransfer rejects pending idempotency reservation")
  void createTransferPendingIdempotencyConflict(VertxTestContext ctx) {
    mockTracing();
    var existing = aTransfer();
    existing.setStatus("pending");
    var req = TransferCommand.CreateTransferRequest.newBuilder()
        .setTransferFrom("4111111111111111")
        .setTransferTo("5111111111111111")
        .setTransferAmount(500_000)
        .setIdempotencyKey("transfer-pending")
        .build();

    when(repo.findByIdempotencyKey(eq("transfer-pending"))).thenReturn(Future.succeededFuture(existing));

    service.createTransfer(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(ConflictException.class)
              .hasMessageContaining("not replayable");
          verify(cardClientRepo, org.mockito.Mockito.never()).findUserCardByCardNumber(anyString());
          verify(saldoClientRepo, org.mockito.Mockito.never()).updateSaldoDelta(anyString(), anyInt());
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("createTransfer fails when insufficient balance")
  void createTransferInsufficientBalance(VertxTestContext ctx) {
    mockTracing();
    var req = TransferCommand.CreateTransferRequest.newBuilder()
        .setTransferFrom("4111111111111111")
        .setTransferTo("5111111111111111")
        .setTransferAmount(2_000_000)
        .build();

    var senderCard = CardWithEmailResponse.newBuilder()
        .setId(1).setUserId(42).setEmail("sender@example.com")
        .setCardNumber("4111111111111111").setCardType("DEBIT")
        .setCvv("123").setExpireDate("2028-12-31T00:00:00Z")
        .setCardProvider("VISA")
        .build();
    var receiverCard = ApiResponseCard.newBuilder()
        .setStatus("success")
        .setData(CardResponse.newBuilder().setId(2).build())
        .build();
    var saldoResp = ApiResponseSaldo.newBuilder()
        .setData(SaldoResponse.newBuilder().setTotalBalance(1_000_000).build())
        .build();

    when(cardClientRepo.findUserCardByCardNumber(eq("4111111111111111")))
        .thenReturn(Future.succeededFuture(senderCard));
    when(cardClientRepo.getCardByCardNumber(eq("5111111111111111")))
        .thenReturn(Future.succeededFuture(receiverCard));
    when(saldoClientRepo.getSaldoByCardNumber(eq("4111111111111111")))
        .thenReturn(Future.succeededFuture(saldoResp));

    service.createTransfer(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(InsufficientBalanceException.class);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("createTransfer re-reads a concurrent insert and replays its success")
  void createTransferConcurrentInsertReplay(VertxTestContext ctx) {
    mockTracing();
    var existing = aTransfer();
    var req = TransferCommand.CreateTransferRequest.newBuilder()
        .setTransferFrom("4111111111111111")
        .setTransferTo("5111111111111111")
        .setTransferAmount(500_000)
        .setIdempotencyKey("transfer-race")
        .build();
    var senderCard = CardWithEmailResponse.newBuilder()
        .setId(1).setUserId(42).setEmail("sender@example.com")
        .setCardNumber("4111111111111111").setCardType("DEBIT")
        .setCvv("123").setExpireDate("2028-12-31T00:00:00Z")
        .setCardProvider("VISA").build();
    var receiverCard = ApiResponseCard.newBuilder()
        .setStatus("success").setData(CardResponse.newBuilder().setId(2).build()).build();
    var saldoResp = ApiResponseSaldo.newBuilder()
        .setData(SaldoResponse.newBuilder().setTotalBalance(1_000_000).build()).build();

    when(repo.findByIdempotencyKey(eq("transfer-race")))
        .thenReturn(Future.succeededFuture(null), Future.succeededFuture(existing));
    when(cardClientRepo.findUserCardByCardNumber(eq("4111111111111111")))
        .thenReturn(Future.succeededFuture(senderCard));
    when(cardClientRepo.getCardByCardNumber(eq("5111111111111111")))
        .thenReturn(Future.succeededFuture(receiverCard));
    when(saldoClientRepo.getSaldoByCardNumber(eq("4111111111111111")))
        .thenReturn(Future.succeededFuture(saldoResp));
    when(repo.createTransfer(eq("4111111111111111"), eq("5111111111111111"), eq(500_000L), eq("transfer-race")))
        .thenReturn(Future.succeededFuture(null));
    stubCacheDeletes();

    service.createTransfer(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(existing.getId());
          verify(cardClientRepo).findUserCardByCardNumber(eq("4111111111111111"));
          verify(saldoClientRepo, org.mockito.Mockito.never()).updateSaldoDelta(anyString(), anyInt());
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("createTransfer marks finalization ambiguity without reversing committed legs")
  void createTransferFinalizationAmbiguityDoesNotReverseBalances(VertxTestContext ctx) {
    mockTracing();
    var transfer = aTransfer();
    var req = TransferCommand.CreateTransferRequest.newBuilder()
        .setTransferFrom("4111111111111111")
        .setTransferTo("5111111111111111")
        .setTransferAmount(500_000)
        .setIdempotencyKey("transfer-finalization-ambiguous")
        .build();
    var senderCard = CardWithEmailResponse.newBuilder()
        .setId(1).setUserId(42).setEmail("sender@example.com")
        .setCardNumber("4111111111111111").setCardType("DEBIT")
        .setCvv("123").setExpireDate("2028-12-31T00:00:00Z")
        .setCardProvider("VISA").build();
    var receiverCard = ApiResponseCard.newBuilder()
        .setStatus("success").setData(CardResponse.newBuilder().setId(2).build()).build();
    var saldoResp = ApiResponseSaldo.newBuilder()
        .setData(SaldoResponse.newBuilder().setTotalBalance(1_000_000).build()).build();
    var finalizationFailure = new RuntimeException("status RPC lost after commit");

    when(repo.findByIdempotencyKey(eq("transfer-finalization-ambiguous")))
        .thenReturn(Future.succeededFuture(null));
    when(cardClientRepo.findUserCardByCardNumber(anyString())).thenReturn(Future.succeededFuture(senderCard));
    when(cardClientRepo.getCardByCardNumber(anyString())).thenReturn(Future.succeededFuture(receiverCard));
    when(saldoClientRepo.getSaldoByCardNumber(anyString())).thenReturn(Future.succeededFuture(saldoResp));
    when(repo.createTransfer(anyString(), anyString(), eq(500_000L), eq("transfer-finalization-ambiguous")))
        .thenReturn(Future.succeededFuture(transfer));
    when(saldoClientRepo.updateSaldoDelta(eq("4111111111111111"), eq(-500_000)))
        .thenReturn(Future.succeededFuture(saldoResp));
    when(saldoClientRepo.updateSaldoDelta(eq("5111111111111111"), eq(500_000)))
        .thenReturn(Future.succeededFuture(saldoResp));
    when(repo.updateTransferStatus(eq(1), eq("success")))
        .thenReturn(Future.failedFuture(finalizationFailure));
    when(repo.updateTransferStatus(eq(1), eq("compensation_required")))
        .thenReturn(Future.succeededFuture(transfer));

    service.createTransfer(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).hasMessageContaining("ambiguous");
          verify(saldoClientRepo).updateSaldoDelta(eq("4111111111111111"), eq(-500_000));
          verify(saldoClientRepo).updateSaldoDelta(eq("5111111111111111"), eq(500_000));
          verify(saldoClientRepo, org.mockito.Mockito.never())
              .updateSaldoDelta(eq("5111111111111111"), eq(-500_000));
          verify(saldoClientRepo, org.mockito.Mockito.never())
              .updateSaldoDelta(eq("4111111111111111"), eq(500_000));
          verify(repo).updateTransferStatus(eq(1), eq("compensation_required"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("createTransfer records compensation_required when sender restore fails")
  void createTransferCompensationRequiredWhenSenderRestoreFails(VertxTestContext ctx) {
    mockTracing();
    var transfer = aTransfer();
    var req = TransferCommand.CreateTransferRequest.newBuilder()
        .setTransferFrom("4111111111111111")
        .setTransferTo("5111111111111111")
        .setTransferAmount(500_000)
        .setIdempotencyKey("transfer-compensation-required")
        .build();
    var senderCard = CardWithEmailResponse.newBuilder()
        .setId(1).setUserId(42).setEmail("sender@example.com")
        .setCardNumber("4111111111111111").setCardType("DEBIT")
        .setCvv("123").setExpireDate("2028-12-31T00:00:00Z")
        .setCardProvider("VISA").build();
    var receiverCard = ApiResponseCard.newBuilder()
        .setStatus("success").setData(CardResponse.newBuilder().setId(2).build()).build();
    var saldoResp = ApiResponseSaldo.newBuilder()
        .setData(SaldoResponse.newBuilder().setTotalBalance(1_000_000).build()).build();
    var receiverFailure = new RuntimeException("receiver unavailable");
    var senderRestoreFailure = new RuntimeException("sender restore unavailable");

    when(repo.findByIdempotencyKey(eq("transfer-compensation-required")))
        .thenReturn(Future.succeededFuture(null));
    when(cardClientRepo.findUserCardByCardNumber(anyString())).thenReturn(Future.succeededFuture(senderCard));
    when(cardClientRepo.getCardByCardNumber(anyString())).thenReturn(Future.succeededFuture(receiverCard));
    when(saldoClientRepo.getSaldoByCardNumber(anyString())).thenReturn(Future.succeededFuture(saldoResp));
    when(repo.createTransfer(anyString(), anyString(), eq(500_000L), eq("transfer-compensation-required")))
        .thenReturn(Future.succeededFuture(transfer));
    when(saldoClientRepo.updateSaldoDelta(eq("4111111111111111"), eq(-500_000)))
        .thenReturn(Future.succeededFuture(saldoResp));
    when(saldoClientRepo.updateSaldoDelta(eq("5111111111111111"), eq(500_000)))
        .thenReturn(Future.failedFuture(receiverFailure));
    when(saldoClientRepo.updateSaldoDelta(eq("4111111111111111"), eq(500_000)))
        .thenReturn(Future.failedFuture(senderRestoreFailure));
    when(repo.updateTransferStatus(eq(1), eq("compensation_required")))
        .thenReturn(Future.succeededFuture(transfer));

    service.createTransfer(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).hasMessageContaining("compensation");
          verify(repo).updateTransferStatus(eq(1), eq("compensation_required"));
          ctx.completeNow();
        })));
  }

  /* ─── updateTransfer ─── */

  @Test
  @DisplayName("updateTransfer updates transfer, sends email, evicts cache")
  void updateTransferSuccess(VertxTestContext ctx) {
    mockTracing();
    var transfer = aTransfer();
    var req = TransferCommand.UpdateTransferRequest.newBuilder()
        .setTransferId(1)
        .setTransferFrom("4111111111111111")
        .setTransferTo("5111111111111111")
        .setTransferAmount(750_000)
        .build();

    var senderCard = CardWithEmailResponse.newBuilder()
        .setId(1).setUserId(42).setEmail("sender@example.com")
        .setCardNumber("4111111111111111").setCardType("DEBIT")
        .setCvv("123").setExpireDate("2028-12-31T00:00:00Z")
        .setCardProvider("VISA")
        .build();

    when(cardClientRepo.findUserCardByCardNumber(eq("4111111111111111")))
        .thenReturn(Future.succeededFuture(senderCard));
    when(repo.updateTransfer(eq(1), eq("4111111111111111"), eq("5111111111111111"), eq(750_000L)))
        .thenReturn(Future.succeededFuture(transfer));
    when(kafkaService.sendMessage(anyString(), anyString(), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture());
    stubCacheDeletes();

    service.updateTransfer(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          assertThat(result.getTransferAmount()).isEqualTo(500_000L);

          verify(cardClientRepo).findUserCardByCardNumber(eq("4111111111111111"));
          verify(repo).updateTransfer(eq(1), eq("4111111111111111"), eq("5111111111111111"), eq(750_000L));
          verify(kafkaService).sendMessage(eq("email-service-topic-transfer-create"), eq("1"), any(JsonObject.class));
          verify(redisService).delete(eq("transfer:1"));
          verify(redisService).delete(eq("transfer:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateTransfer fails when transfer not found")
  void updateTransferNotFound(VertxTestContext ctx) {
    mockTracing();
    var req = TransferCommand.UpdateTransferRequest.newBuilder()
        .setTransferId(99)
        .setTransferFrom("4111111111111111")
        .setTransferTo("5111111111111111")
        .setTransferAmount(500_000)
        .build();

    var senderCard = CardWithEmailResponse.newBuilder()
        .setId(1).setUserId(42).setEmail("sender@example.com")
        .setCardNumber("4111111111111111").setCardType("DEBIT")
        .setCvv("123").setExpireDate("2028-12-31T00:00:00Z")
        .setCardProvider("VISA")
        .build();

    when(cardClientRepo.findUserCardByCardNumber(eq("4111111111111111")))
        .thenReturn(Future.succeededFuture(senderCard));
    when(repo.updateTransfer(eq(99), eq("4111111111111111"), eq("5111111111111111"), eq(500_000L)))
        .thenReturn(Future.succeededFuture(null));

    service.updateTransfer(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }

  /* ─── trashTransfer ─── */

  @Test
  @DisplayName("trashTransfer soft-deletes and evicts cache")
  void trashTransferSuccess(VertxTestContext ctx) {
    mockTracing();
    var transfer = aTransfer();

    when(repo.trashTransfer(1)).thenReturn(Future.succeededFuture(transfer));
    stubCacheDeletes();

    service.trashTransfer(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          verify(repo).trashTransfer(1);
          verify(redisService).delete(eq("transfer:1"));
          verify(redisService).delete(eq("transfer:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("trashTransfer fails when transfer not found")
  void trashTransferNotFound(VertxTestContext ctx) {
    mockTracing();

    when(repo.trashTransfer(99)).thenReturn(Future.succeededFuture(null));

    service.trashTransfer(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }

  /* ─── restoreTransfer ─── */

  @Test
  @DisplayName("restoreTransfer restores trashed transfer and evicts cache")
  void restoreTransferSuccess(VertxTestContext ctx) {
    mockTracing();
    var trashed = aTransfer();
    trashed.setDeletedAt(OffsetDateTime.of(2026, 6, 25, 10, 0, 0, 0, ZoneOffset.UTC));
    var restored = aTransfer();

    when(queryRepository.findByTrashedId(1)).thenReturn(Future.succeededFuture(trashed));
    when(repo.restoreTransfer(1)).thenReturn(Future.succeededFuture(restored));
    stubCacheDeletes();

    service.restoreTransfer(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          verify(queryRepository).findByTrashedId(1);
          verify(repo).restoreTransfer(1);
          verify(redisService).delete(eq("transfer:1"));
          verify(redisService).delete(eq("transfer:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("restoreTransfer fails when transfer is not trashed")
  void restoreTransferNotTrashed(VertxTestContext ctx) {
    mockTracing();

    when(queryRepository.findByTrashedId(99)).thenReturn(Future.succeededFuture(null));

    service.restoreTransfer(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class)
              .hasMessage("Transfer not found or must be trashed first");
          ctx.completeNow();
        })));
  }

  /* ─── deleteTransferPermanently ─── */

  @Test
  @DisplayName("deleteTransferPermanently deletes trashed transfer and evicts cache")
  void deleteTransferPermanentlySuccess(VertxTestContext ctx) {
    mockTracing();
    var trashed = aTransfer();
    trashed.setDeletedAt(OffsetDateTime.of(2026, 6, 25, 10, 0, 0, 0, ZoneOffset.UTC));

    when(queryRepository.findByTrashedId(1)).thenReturn(Future.succeededFuture(trashed));
    when(repo.deleteTransferPermanently(1)).thenReturn(Future.succeededFuture(true));
    stubCacheDeletes();

    service.deleteTransferPermanently(1)
        .onComplete(ctx.succeeding(v -> ctx.verify(() -> {
          verify(queryRepository).findByTrashedId(1);
          verify(repo).deleteTransferPermanently(1);
          verify(redisService).delete(eq("transfer:1"));
          verify(redisService).delete(eq("transfer:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deleteTransferPermanently fails when transfer is not trashed")
  void deleteTransferPermanentlyNotTrashed(VertxTestContext ctx) {
    mockTracing();

    when(queryRepository.findByTrashedId(99)).thenReturn(Future.succeededFuture(null));

    service.deleteTransferPermanently(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class);
          ctx.completeNow();
        })));
  }

  /* ─── restoreAllTransfers ─── */

  @Test
  @DisplayName("restoreAllTransfers restores all trashed transfers and evicts list cache")
  void restoreAllTransfersSuccess(VertxTestContext ctx) {
    mockTracing();

    when(repo.restoreAllTransfers()).thenReturn(Future.succeededFuture(3));
    stubCacheDeletes();

    service.restoreAllTransfers()
        .onComplete(ctx.succeeding(v -> ctx.verify(() -> {
          verify(repo).restoreAllTransfers();
          verify(redisService).delete(eq("transfer:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("restoreAllTransfers fails when no trashed transfers found")
  void restoreAllTransfersNone(VertxTestContext ctx) {
    mockTracing();

    when(repo.restoreAllTransfers()).thenReturn(Future.succeededFuture(0));

    service.restoreAllTransfers()
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class)
              .hasMessage("No trashed transfers found");
          ctx.completeNow();
        })));
  }

  /* ─── deleteAllPermanentTransfers ─── */

  @Test
  @DisplayName("deleteAllPermanentTransfers deletes all trashed transfers and evicts list cache")
  void deleteAllPermanentTransfersSuccess(VertxTestContext ctx) {
    mockTracing();

    when(repo.deleteAllPermanentTransfers()).thenReturn(Future.succeededFuture(2));
    stubCacheDeletes();

    service.deleteAllPermanentTransfers()
        .onComplete(ctx.succeeding(v -> ctx.verify(() -> {
          verify(repo).deleteAllPermanentTransfers();
          verify(redisService).delete(eq("transfer:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deleteAllPermanentTransfers fails when no trashed transfers found")
  void deleteAllPermanentTransfersNone(VertxTestContext ctx) {
    mockTracing();

    when(repo.deleteAllPermanentTransfers()).thenReturn(Future.succeededFuture(0));

    service.deleteAllPermanentTransfers()
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class)
              .hasMessage("No trashed transfers found");
          ctx.completeNow();
        })));
  }
}
