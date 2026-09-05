package io.example.card.service.impl;

import io.example.card.model.CardAuthTransaction;
import io.example.card.model.CardCreditAccount;
import io.example.card.repository.CardAuthTransactionRepository;
import io.example.card.repository.CardCreditAccountRepository;
import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.ConflictException;
import io.example.common.exception.grpc.FailedPreconditionException;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class CardAuthorizationServiceImplTest {

  @Mock
  private CardCreditAccountRepository creditAccountRepo;

  @Mock
  private CardAuthTransactionRepository authTxnRepo;

  @Mock
  private RedisService redis;

  @Mock
  private TracingMetrics metrics;

  @Mock
  private KafkaService kafkaService;

  private CardAuthorizationServiceImpl service;
  private final UUID txnUuid = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service = new CardAuthorizationServiceImpl(creditAccountRepo, authTxnRepo, redis, metrics, kafkaService);
  }

  private void mockTracing() {
    var ctx = new TracingContext(Context.root(), Instant.now());
    lenient().when(metrics.startSpan(anyString())).thenReturn(ctx);
    lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(ctx);
  }

  private CardCreditAccount aCreditAccount(String status, Long limit, Long used) {
    return CardCreditAccount.builder()
        .cardNumber("4111111111111111")
        .creditLimit(limit)
        .usedCredit(used)
        .availableCredit(limit - used)
        .status(status)
        .build();
  }

  private CardAuthTransaction aTxn(String status) {
    return CardAuthTransaction.builder()
        .txnId(txnUuid)
        .cardNumber("4111111111111111")
        .amount(1000L)
        .currency("IDR")
        .status(status)
        .idempotencyKey("idem-123")
        .build();
  }

  @Test
  @DisplayName("authorize approved flow succeeds")
  void authorizeSuccess(VertxTestContext ctx) {
    mockTracing();
    var account = aCreditAccount("ACTIVE", 10000L, 2000L);
    var pendingTxn = aTxn("PENDING");
    var approvedTxn = aTxn("APPROVED");
    approvedTxn.setAuthCode("AUTH123");

    when(authTxnRepo.findByIdempotencyKey("idem-123")).thenReturn(Future.succeededFuture(null));
    when(redis.get("card:4111111111111111:credit")).thenReturn(Future.succeededFuture(null));
    when(creditAccountRepo.findByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(account));
    when(redis.set(anyString(), anyString(), any(Duration.class))).thenReturn(Future.succeededFuture());
    when(redis.incr("card:4111111111111111:velocity:60s")).thenReturn(Future.succeededFuture(1L));
    when(redis.expire(eq("card:4111111111111111:velocity:60s"), any(Duration.class))).thenReturn(Future.succeededFuture());
    when(authTxnRepo.insertPending(any())).thenReturn(Future.succeededFuture(pendingTxn));
    when(creditAccountRepo.decrementAvailableCredit("4111111111111111", 1000L)).thenReturn(Future.succeededFuture(account));
    when(authTxnRepo.approve(eq(txnUuid.toString()), anyString())).thenReturn(Future.succeededFuture(approvedTxn));
    when(redis.delete("card:4111111111111111:credit")).thenReturn(Future.succeededFuture(1L));
    when(kafkaService.sendMessage(eq("card.txn.created"), anyString(), any())).thenReturn(Future.succeededFuture());

    service.authorize("4111111111111111", 123, 1000L, "IDR", "01", "5411", "idem-123")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getStatus()).isEqualTo("APPROVED");
          assertThat(result.getAuthCode()).isEqualTo("AUTH123");
          verify(kafkaService).sendMessage(eq("card.txn.created"), eq(txnUuid.toString()), any());
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("authorize duplicate idempotency key fails with ConflictException")
  void authorizeDuplicateIdempotency(VertxTestContext ctx) {
    mockTracing();
    var existing = aTxn("APPROVED");

    when(authTxnRepo.findByIdempotencyKey("idem-123")).thenReturn(Future.succeededFuture(existing));

    service.authorize("4111111111111111", 123, 1000L, "IDR", "01", "5411", "idem-123")
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(ConflictException.class);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("authorize inactive credit account fails with FailedPreconditionException")
  void authorizeInactiveAccount(VertxTestContext ctx) {
    mockTracing();
    var account = aCreditAccount("SUSPENDED", 10000L, 2000L);

    when(authTxnRepo.findByIdempotencyKey("idem-123")).thenReturn(Future.succeededFuture(null));
    when(redis.get("card:4111111111111111:credit")).thenReturn(Future.succeededFuture(null));
    when(creditAccountRepo.findByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(account));
    when(redis.set(anyString(), anyString(), any(Duration.class))).thenReturn(Future.succeededFuture());

    service.authorize("4111111111111111", 123, 1000L, "IDR", "01", "5411", "idem-123")
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(FailedPreconditionException.class)
              .hasMessageContaining("Card status: SUSPENDED");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("authorize insufficient credit limit fails with FailedPreconditionException")
  void authorizeInsufficientCredit(VertxTestContext ctx) {
    mockTracing();
    var account = aCreditAccount("ACTIVE", 5000L, 4500L); // 500 avail

    when(authTxnRepo.findByIdempotencyKey("idem-123")).thenReturn(Future.succeededFuture(null));
    when(redis.get("card:4111111111111111:credit")).thenReturn(Future.succeededFuture(null));
    when(creditAccountRepo.findByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(account));
    when(redis.set(anyString(), anyString(), any(Duration.class))).thenReturn(Future.succeededFuture());

    service.authorize("4111111111111111", 123, 1000L, "IDR", "01", "5411", "idem-123")
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(FailedPreconditionException.class)
              .hasMessageContaining("Insufficient available credit");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("authorize velocity limit exceeded fails with FailedPreconditionException")
  void authorizeVelocityExceeded(VertxTestContext ctx) {
    mockTracing();
    var account = aCreditAccount("ACTIVE", 10000L, 2000L);

    when(authTxnRepo.findByIdempotencyKey("idem-123")).thenReturn(Future.succeededFuture(null));
    when(redis.get("card:4111111111111111:credit")).thenReturn(Future.succeededFuture(null));
    when(creditAccountRepo.findByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(account));
    when(redis.set(anyString(), anyString(), any(Duration.class))).thenReturn(Future.succeededFuture());
    when(redis.incr("card:4111111111111111:velocity:60s")).thenReturn(Future.succeededFuture(11L)); // limit is 10

    service.authorize("4111111111111111", 123, 1000L, "IDR", "01", "5411", "idem-123")
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(FailedPreconditionException.class)
              .hasMessageContaining("Velocity limit exceeded");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("reverse approved txn succeeds")
  void reverseSuccess(VertxTestContext ctx) {
    mockTracing();
    var txn = aTxn("APPROVED");
    var reversedTxn = aTxn("REVERSED");
    var account = aCreditAccount("ACTIVE", 10000L, 2000L);

    when(authTxnRepo.findById(txnUuid.toString())).thenReturn(Future.succeededFuture(txn));
    when(creditAccountRepo.releaseCredit("4111111111111111", 1000L)).thenReturn(Future.succeededFuture(account));
    when(authTxnRepo.reverse(txnUuid.toString())).thenReturn(Future.succeededFuture(reversedTxn));
    when(redis.delete("card:4111111111111111:credit")).thenReturn(Future.succeededFuture(1L));

    service.reverse(txnUuid.toString(), "4111111111111111", 1000L, "idem-123")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getStatus()).isEqualTo("REVERSED");
          verify(redis).delete("card:4111111111111111:credit");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("reverse not found txn fails with BadRequestException")
  void reverseNotFound(VertxTestContext ctx) {
    mockTracing();
    when(authTxnRepo.findById(txnUuid.toString())).thenReturn(Future.succeededFuture(null));

    service.reverse(txnUuid.toString(), "4111111111111111", 1000L, "idem-123")
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class)
              .hasMessageContaining("Transaction not found");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("reverse non-approved txn fails with BadRequestException")
  void reverseWrongStatus(VertxTestContext ctx) {
    mockTracing();
    var txn = aTxn("DECLINED");
    when(authTxnRepo.findById(txnUuid.toString())).thenReturn(Future.succeededFuture(txn));

    service.reverse(txnUuid.toString(), "4111111111111111", 1000L, "idem-123")
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class)
              .hasMessageContaining("Transaction cannot be reversed, status: DECLINED");
          ctx.completeNow();
        })));
  }
}
