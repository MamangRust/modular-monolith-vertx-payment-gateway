package io.example.transaction.service.impl;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.ConflictException;
import io.example.common.exception.grpc.ForbiddenException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.example.transaction.model.Transaction;
import io.example.transaction.model.TransactionResponse;
import io.example.transaction.model.TransactionResponseDeleteAt;
import io.example.transaction.repository.CardClientRepository;
import io.example.transaction.repository.MerchantClientRepository;
import io.example.transaction.repository.SaldoClientRepository;
import io.example.transaction.repository.TransactionCommandRepository;
import io.example.transaction.repository.TransactionQueryRepository;
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
import pb.merchant.Merchant.ApiResponseMerchant;
import pb.merchant.Merchant.MerchantResponse;
import pb.saldo.Saldo.ApiResponseSaldo;
import pb.saldo.Saldo.SaldoResponse;
import pb.transaction.TransactionCommand.CreateTransactionRequest;
import pb.transaction.TransactionCommand.UpdateTransactionRequest;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionCommandServiceImplTest {

  @Mock
  private TransactionCommandRepository repo;

  @Mock
  private TransactionQueryRepository queryRepository;

  @Mock
  private MerchantClientRepository repoMerchant;

  @Mock
  private CardClientRepository repoCard;

  @Mock
  private SaldoClientRepository repoSaldo;

  @Mock
  private RedisService redisService;

  @Mock
  private KafkaService kafkaService;

  @Mock
  private TracingMetrics tracingMetrics;

  private TransactionCommandServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new TransactionCommandServiceImpl(repo, queryRepository, repoMerchant, repoCard, repoSaldo, redisService, kafkaService, tracingMetrics);
  }

  private void mockTracing() {
    var tc = new TracingContext(Context.root(), Instant.now());
    lenient().when(tracingMetrics.startSpan(anyString())).thenReturn(tc);
    lenient().when(tracingMetrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tc);
  }

  private OffsetDateTime now() {
    return OffsetDateTime.of(2026, 6, 26, 10, 0, 0, 0, ZoneOffset.UTC);
  }

  private Transaction aTransaction() {
    return Transaction.builder()
        .id(1)
        .transactionNo("a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        .cardNumber("4111111111111111")
        .amount(500_000L)
        .paymentMethod("BANK")
        .merchantId(1)
        .status("success")
        .transactionTime(now())
        .createdAt(now())
        .updatedAt(now())
        .build();
  }

  private void stubCacheInvalidation() {
    when(redisService.delete(anyString())).thenReturn(Future.succeededFuture(1L));
  }

  private MerchantResponse aMerchant() {
    return MerchantResponse.newBuilder().setId(1).setUserId(42).build();
  }

  private ApiResponseMerchant aMerchantApiResp() {
    return ApiResponseMerchant.newBuilder().setStatus("success").setData(aMerchant()).build();
  }

  private ApiResponseSaldo aSaldoResp(int balance) {
    return ApiResponseSaldo.newBuilder()
        .setStatus("success")
        .setData(SaldoResponse.newBuilder().setTotalBalance(balance).build())
        .build();
  }

  private ApiResponseCard aCardResp() {
    return ApiResponseCard.newBuilder()
        .setStatus("success")
        .setData(CardResponse.newBuilder().setId(1).setCardNumber("4111111111111111").build())
        .build();
  }

  /* ─── createTransaction ─── */

  @Test
  @DisplayName("createTransaction creates transaction, deducts saldo, credits merchant, sends email")
  void createTransactionSuccess(VertxTestContext ctx) {
    mockTracing();
    var transaction = aTransaction();

    var req = CreateTransactionRequest.newBuilder()
        .setApiKey("key-123")
        .setCardNumber("4111111111111111")
        .setAmount(500_000)
        .setPaymentMethod("BANK")
        .setIdempotencyKey("txn-idem-success")
        .build();

    var cardEmail = CardWithEmailResponse.newBuilder()
        .setId(1).setUserId(42).setEmail("test@example.com")
        .setCardNumber("4111111111111111").setCardType("CREDIT")
        .setCvv("123").setExpireDate("2028-12-31T00:00:00Z")
        .setCardProvider("VISA")
        .build();

    when(repo.findByIdempotencyKey("txn-idem-success")).thenReturn(Future.succeededFuture(null));
    when(repoMerchant.getMerchantByApiKey("key-123")).thenReturn(Future.succeededFuture(aMerchantApiResp()));
    when(repoCard.getUserCardByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(cardEmail));
    when(repoSaldo.getSaldoByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(aSaldoResp(1_000_000)));
    when(repoSaldo.updateSaldoDelta(eq("4111111111111111"), anyInt())).thenReturn(Future.succeededFuture(aSaldoResp(500_000)));
    when(repoSaldo.updateSaldoBalance(eq("4111111111111111"), anyInt())).thenReturn(Future.succeededFuture(aSaldoResp(500_000)));
    when(repo.createTransaction(any(CreateTransactionRequest.class))).thenReturn(Future.succeededFuture(transaction));
    when(repo.updateTransactionStatus(1, "success")).thenReturn(Future.succeededFuture(transaction));
    when(repoCard.getCardByUserId(42)).thenReturn(Future.succeededFuture(aCardResp()));
    when(kafkaService.sendMessage(anyString(), anyString(), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture());

    service.createTransaction(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
          assertThat(result.getAmount()).isEqualTo(500_000);
          assertThat(result.getStatus()).isEqualTo("success");

          verify(repoMerchant).getMerchantByApiKey(eq("key-123"));
          verify(repoCard).getUserCardByCardNumber(eq("4111111111111111"));
          verify(repoSaldo).getSaldoByCardNumber(eq("4111111111111111"));
          verify(repoSaldo).updateSaldoDelta(eq("4111111111111111"), eq(-500_000));
          verify(repoSaldo).updateSaldoDelta(eq("4111111111111111"), eq(500_000));
          verify(repo).createTransaction(any(CreateTransactionRequest.class));
          verify(repo).updateTransactionStatus(eq(1), eq("success"));
          verify(repoCard).getCardByUserId(eq(42));
          verify(kafkaService).sendMessage(eq("email-service-topic-transaction-create"), eq("1"), any(JsonObject.class));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("createTransaction replays existing transaction for the same idempotency key")
  void createTransactionIdempotentReplay(VertxTestContext ctx) {
    mockTracing();
    var existing = aTransaction();
    var req = CreateTransactionRequest.newBuilder()
        .setApiKey("key-123")
        .setCardNumber(existing.getCardNumber())
        .setAmount(existing.getAmount().intValue())
        .setPaymentMethod(existing.getPaymentMethod())
        .setMerchantId(existing.getMerchantId())
        .setIdempotencyKey("txn-idem-123")
        .build();

    when(repo.findByIdempotencyKey("txn-idem-123")).thenReturn(Future.succeededFuture(existing));
    when(repoMerchant.getMerchantByApiKey("key-123")).thenReturn(Future.succeededFuture(aMerchantApiResp()));

    service.createTransaction(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(existing.getId());
          verify(repo).findByIdempotencyKey(eq("txn-idem-123"));
          verify(repo, times(0)).createTransaction(any(CreateTransactionRequest.class));
          verify(repoMerchant).getMerchantByApiKey(eq("key-123"));
          verifyNoInteractions(repoCard, repoSaldo, kafkaService);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("createTransaction rejects an idempotency key reused with different request data")
  void createTransactionIdempotencyConflict(VertxTestContext ctx) {
    mockTracing();
    var existing = aTransaction();
    var req = CreateTransactionRequest.newBuilder()
        .setApiKey("key-123")
        .setCardNumber(existing.getCardNumber())
        .setAmount(existing.getAmount().intValue() + 1)
        .setPaymentMethod(existing.getPaymentMethod())
        .setMerchantId(existing.getMerchantId())
        .setIdempotencyKey("txn-idem-123")
        .build();

    when(repo.findByIdempotencyKey("txn-idem-123")).thenReturn(Future.succeededFuture(existing));
    when(repoMerchant.getMerchantByApiKey("key-123")).thenReturn(Future.succeededFuture(aMerchantApiResp()));

    service.createTransaction(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(ConflictException.class);
          verify(repo).findByIdempotencyKey(eq("txn-idem-123"));
          verify(repo, times(0)).createTransaction(any(CreateTransactionRequest.class));
          verify(repoMerchant).getMerchantByApiKey(eq("key-123"));
          verifyNoInteractions(repoCard, repoSaldo, kafkaService);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("createTransaction fails when balance is insufficient")
  void createTransactionInsufficientBalance(VertxTestContext ctx) {
    mockTracing();

    var req = CreateTransactionRequest.newBuilder()
        .setApiKey("key-123")
        .setCardNumber("4111111111111111")
        .setAmount(500_000)
        .setPaymentMethod("BANK")
        .setIdempotencyKey("txn-idem-insufficient")
        .build();

    var cardEmail = CardWithEmailResponse.newBuilder()
        .setId(1).setUserId(42).setEmail("test@example.com")
        .setCardNumber("4111111111111111").setCardType("CREDIT")
        .setCvv("123").setExpireDate("2028-12-31T00:00:00Z")
        .setCardProvider("VISA")
        .build();

    when(repo.findByIdempotencyKey("txn-idem-insufficient")).thenReturn(Future.succeededFuture(null));
    when(repoMerchant.getMerchantByApiKey("key-123")).thenReturn(Future.succeededFuture(aMerchantApiResp()));
    when(repoCard.getUserCardByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(cardEmail));
    when(repoSaldo.getSaldoByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(aSaldoResp(100_000)));
    when(repo.createTransaction(any(CreateTransactionRequest.class))).thenReturn(Future.succeededFuture(aTransaction()));
    when(repo.updateTransactionStatus(eq(1), eq("failed"))).thenReturn(Future.succeededFuture(aTransaction()));

    service.createTransaction(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("createTransaction compensates user debit when merchant credit fails")
  void createTransactionCompensatesWhenMerchantCreditFails(VertxTestContext ctx) {
    mockTracing();
    var transaction = aTransaction();
    var req = CreateTransactionRequest.newBuilder()
        .setApiKey("key-123")
        .setCardNumber("4111111111111111")
        .setAmount(500_000)
        .setPaymentMethod("BANK")
        .setIdempotencyKey("txn-idem-credit-failure")
        .build();
    var userCard = CardWithEmailResponse.newBuilder()
        .setId(1).setUserId(42).setEmail("user@example.com")
        .setCardNumber("4111111111111111").setCardType("CREDIT")
        .setCvv("123").setExpireDate("2028-12-31T00:00:00Z")
        .setCardProvider("VISA").build();
    var merchantCard = ApiResponseCard.newBuilder()
        .setStatus("success")
        .setData(CardResponse.newBuilder().setId(2).setCardNumber("5111111111111111").build())
        .build();
    var creditFailure = new RuntimeException("merchant saldo unavailable");

    when(repo.findByIdempotencyKey("txn-idem-credit-failure")).thenReturn(Future.succeededFuture(null));
    when(repoMerchant.getMerchantByApiKey("key-123")).thenReturn(Future.succeededFuture(aMerchantApiResp()));
    when(repoCard.getUserCardByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(userCard));
    when(repoCard.getCardByUserId(42)).thenReturn(Future.succeededFuture(merchantCard));
    when(repoSaldo.getSaldoByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(aSaldoResp(1_000_000)));
    when(repoSaldo.updateSaldoDelta("4111111111111111", -500_000)).thenReturn(Future.succeededFuture(aSaldoResp(500_000)));
    when(repoSaldo.updateSaldoDelta("5111111111111111", 500_000)).thenReturn(Future.failedFuture(creditFailure));
    when(repoSaldo.updateSaldoDelta("4111111111111111", 500_000)).thenReturn(Future.succeededFuture(aSaldoResp(1_000_000)));
    when(repo.createTransaction(any(CreateTransactionRequest.class))).thenReturn(Future.succeededFuture(transaction));
    when(repo.updateTransactionStatus(1, "success")).thenReturn(Future.succeededFuture(transaction));
    when(repo.updateTransactionStatus(1, "failed")).thenReturn(Future.succeededFuture(transaction));

    service.createTransaction(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isSameAs(creditFailure);
          verify(repoSaldo).updateSaldoDelta(eq("4111111111111111"), eq(-500_000));
          verify(repoSaldo).updateSaldoDelta(eq("5111111111111111"), eq(500_000));
          verify(repoSaldo).updateSaldoDelta(eq("4111111111111111"), eq(500_000));
          verify(repo).updateTransactionStatus(eq(1), eq("failed"));
          ctx.completeNow();
        })));
  }

  /* ─── updateTransaction ─── */

  @Test
  @DisplayName("updateTransaction updates transaction, adjusts saldo, evicts cache")
  void updateTransactionSuccess(VertxTestContext ctx) {
    mockTracing();
    var existing = aTransaction();
    var updated = Transaction.builder()
        .id(1)
        .transactionNo("a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        .cardNumber("4111111111111111")
        .amount(750_000L)
        .paymentMethod("BANK")
        .merchantId(1)
        .status("success")
        .transactionTime(now())
        .createdAt(now())
        .updatedAt(now())
        .build();

    var req = UpdateTransactionRequest.newBuilder()
        .setTransactionId(1)
        .setApiKey("key-123")
        .setCardNumber("4111111111111111")
        .setAmount(750_000)
        .setPaymentMethod("BANK")
        .build();

    when(repo.getTransactionById(1)).thenReturn(Future.succeededFuture(existing));
    when(repoMerchant.getMerchantByApiKey("key-123")).thenReturn(Future.succeededFuture(aMerchantApiResp()));
    when(repoCard.getCardByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(aCardResp()));
    when(repoSaldo.getSaldoByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(aSaldoResp(1_000_000)));
    when(repoSaldo.updateSaldoBalance(eq("4111111111111111"), anyInt())).thenReturn(Future.succeededFuture(aSaldoResp(750_000)));
    when(repo.updateTransaction(any(UpdateTransactionRequest.class))).thenReturn(Future.succeededFuture(updated));
    when(repo.updateTransactionStatus(1, "success")).thenReturn(Future.succeededFuture(updated));
    stubCacheInvalidation();

    service.updateTransaction(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          assertThat(result.getAmount()).isEqualTo(750_000);

          verify(repo).getTransactionById(1);
          verify(repoMerchant).getMerchantByApiKey(eq("key-123"));
          verify(repoCard).getCardByCardNumber(eq("4111111111111111"));
          verify(repo).updateTransaction(any(UpdateTransactionRequest.class));
          verify(repo).updateTransactionStatus(eq(1), eq("success"));
          verify(redisService).delete(eq("transaction:1"));
          verify(redisService).delete(eq("transaction:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateTransaction fails when transaction does not belong to merchant")
  void updateTransactionForbidden(VertxTestContext ctx) {
    mockTracing();
    var existing = aTransaction(); // merchantId = 1
    // Make existing merchantId differ from request merchant
    var differentMerchant = MerchantResponse.newBuilder().setId(99).setUserId(42).build();
    var merchantResp = ApiResponseMerchant.newBuilder().setStatus("success").setData(differentMerchant).build();

    var req = UpdateTransactionRequest.newBuilder()
        .setTransactionId(1)
        .setApiKey("key-123")
        .setCardNumber("4111111111111111")
        .setAmount(750_000)
        .build();

    when(repo.getTransactionById(1)).thenReturn(Future.succeededFuture(existing));
    when(repoMerchant.getMerchantByApiKey("key-123")).thenReturn(Future.succeededFuture(merchantResp));

    service.updateTransaction(req)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(ForbiddenException.class);
          ctx.completeNow();
        })));
  }

  /* ─── trashTransaction ─── */

  @Test
  @DisplayName("trashTransaction soft-deletes and evicts cache")
  void trashTransactionSuccess(VertxTestContext ctx) {
    mockTracing();
    var transaction = aTransaction();

    when(repo.trashed(1)).thenReturn(Future.succeededFuture(transaction));
    stubCacheInvalidation();

    service.trashTransaction(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          verify(repo).trashed(1);
          verify(redisService).delete(eq("transaction:1"));
          verify(redisService).delete(eq("transaction:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("trashTransaction fails when transaction not found")
  void trashTransactionNotFound(VertxTestContext ctx) {
    mockTracing();

    when(repo.trashed(99)).thenReturn(Future.succeededFuture(null));

    service.trashTransaction(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }

  /* ─── restoreTransaction ─── */

  @Test
  @DisplayName("restoreTransaction restores trashed transaction and evicts cache")
  void restoreTransactionSuccess(VertxTestContext ctx) {
    mockTracing();
    var trashed = aTransaction();
    trashed.setDeletedAt(OffsetDateTime.of(2026, 6, 25, 10, 0, 0, 0, ZoneOffset.UTC));
    var restored = aTransaction();

    when(queryRepository.findByTrashed(1)).thenReturn(Future.succeededFuture(trashed));
    when(repo.restoreTransaction(1)).thenReturn(Future.succeededFuture(restored));
    stubCacheInvalidation();

    service.restoreTransaction(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          verify(queryRepository).findByTrashed(1);
          verify(repo).restoreTransaction(1);
          verify(redisService).delete(eq("transaction:1"));
          verify(redisService).delete(eq("transaction:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("restoreTransaction fails when transaction is not trashed")
  void restoreTransactionNotTrashed(VertxTestContext ctx) {
    mockTracing();

    when(queryRepository.findByTrashed(99)).thenReturn(Future.succeededFuture(null));

    service.restoreTransaction(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class)
              .hasMessage("Transaction not found or must be trashed first");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("restoreTransaction fails when restore returns null")
  void restoreTransactionNotFound(VertxTestContext ctx) {
    mockTracing();
    var trashed = aTransaction();
    trashed.setDeletedAt(OffsetDateTime.of(2026, 6, 25, 10, 0, 0, 0, ZoneOffset.UTC));

    when(queryRepository.findByTrashed(1)).thenReturn(Future.succeededFuture(trashed));
    when(repo.restoreTransaction(1)).thenReturn(Future.succeededFuture(null));

    service.restoreTransaction(1)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }

  /* ─── deleteTransactionPermanently ─── */

  @Test
  @DisplayName("deleteTransactionPermanently deletes trashed transaction and evicts cache")
  void deleteTransactionPermanentlySuccess(VertxTestContext ctx) {
    mockTracing();
    var trashed = aTransaction();
    trashed.setDeletedAt(OffsetDateTime.of(2026, 6, 25, 10, 0, 0, 0, ZoneOffset.UTC));

    when(queryRepository.findByTrashed(1)).thenReturn(Future.succeededFuture(trashed));
    when(repo.deletePermanently(1)).thenReturn(Future.succeededFuture(true));
    stubCacheInvalidation();

    service.deleteTransactionPermanently(1)
        .onComplete(ctx.succeeding(v -> ctx.verify(() -> {
          verify(queryRepository).findByTrashed(1);
          verify(repo).deletePermanently(1);
          verify(redisService).delete(eq("transaction:1"));
          verify(redisService).delete(eq("transaction:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deleteTransactionPermanently fails when transaction is not trashed")
  void deleteTransactionPermanentlyNotTrashed(VertxTestContext ctx) {
    mockTracing();

    when(queryRepository.findByTrashed(99)).thenReturn(Future.succeededFuture(null));

    service.deleteTransactionPermanently(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deleteTransactionPermanently fails when repo returns false")
  void deleteTransactionPermanentlyNotDeleted(VertxTestContext ctx) {
    mockTracing();
    var trashed = aTransaction();
    trashed.setDeletedAt(OffsetDateTime.of(2026, 6, 25, 10, 0, 0, 0, ZoneOffset.UTC));

    when(queryRepository.findByTrashed(1)).thenReturn(Future.succeededFuture(trashed));
    when(repo.deletePermanently(1)).thenReturn(Future.succeededFuture(false));

    service.deleteTransactionPermanently(1)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class);
          ctx.completeNow();
        })));
  }

  /* ─── restoreAllTransactions ─── */

  @Test
  @DisplayName("restoreAllTransactions restores all trashed transactions and evicts list cache")
  void restoreAllTransactionsSuccess(VertxTestContext ctx) {
    mockTracing();

    when(repo.restoreAllTransactions()).thenReturn(Future.succeededFuture(3));
    stubCacheInvalidation();

    service.restoreAllTransactions()
        .onComplete(ctx.succeeding(v -> ctx.verify(() -> {
          verify(repo).restoreAllTransactions();
          verify(redisService).delete(eq("transaction:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("restoreAllTransactions fails when no trashed transactions found")
  void restoreAllTransactionsNone(VertxTestContext ctx) {
    mockTracing();

    when(repo.restoreAllTransactions()).thenReturn(Future.succeededFuture(0));

    service.restoreAllTransactions()
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class)
              .hasMessage("No trashed transactions found");
          ctx.completeNow();
        })));
  }

  /* ─── deleteAllPermanentTransactions ─── */

  @Test
  @DisplayName("deleteAllPermanentTransactions deletes all trashed transactions and evicts list cache")
  void deleteAllPermanentTransactionsSuccess(VertxTestContext ctx) {
    mockTracing();

    when(repo.deleteAllPermanentTransactions()).thenReturn(Future.succeededFuture(2));
    stubCacheInvalidation();

    service.deleteAllPermanentTransactions()
        .onComplete(ctx.succeeding(v -> ctx.verify(() -> {
          verify(repo).deleteAllPermanentTransactions();
          verify(redisService).delete(eq("transaction:list:*"));
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deleteAllPermanentTransactions fails when no trashed transactions found")
  void deleteAllPermanentTransactionsNone(VertxTestContext ctx) {
    mockTracing();

    when(repo.deleteAllPermanentTransactions()).thenReturn(Future.succeededFuture(0));

    service.deleteAllPermanentTransactions()
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class)
              .hasMessage("No trashed transactions found");
          ctx.completeNow();
        })));
  }
}
