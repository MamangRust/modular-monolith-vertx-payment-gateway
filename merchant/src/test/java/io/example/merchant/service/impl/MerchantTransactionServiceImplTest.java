package io.example.merchant.service.impl;

import io.example.common.domain.PagedResult;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.merchant.model.MerchantTransactions;
import io.example.merchant.repository.MerchantTransactionRepository;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.merchant.Merchant.FindAllMerchantTransaction;
import pb.merchant.Merchant.FindAllMerchantTransactionApikey;
import pb.merchant.Merchant.FindAllMerchantTransactionId;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class MerchantTransactionServiceImplTest {

  @Mock
  private MerchantTransactionRepository repo;

  @Mock
  private RedisService redis;

  @Mock
  private TracingMetrics tracingMetrics;

  private MerchantTransactionServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new MerchantTransactionServiceImpl(repo, redis, tracingMetrics);
  }

  private void mockTracing() {
    var tc = new TracingContext(io.opentelemetry.context.Context.root(), java.time.Instant.now());
    when(tracingMetrics.startSpan(any(String.class))).thenReturn(tc);
    when(tracingMetrics.startSpan(any(String.class), any())).thenReturn(tc);
  }

  private static MerchantTransactions aTxn(int id) {
    return MerchantTransactions.builder()
        .transactionId(id).cardNumber("1234").amount(1000L).paymentMethod("CREDIT")
        .merchantId(10).merchantName("Test").transactionTime(Timestamp.from(Instant.now())).build();
  }

  @Test
  @DisplayName("getTransactions fetches from cache hit")
  void getTransactionsCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        {"data":[{"transaction_id":100,"card_number":"1234","amount":1000,"payment_method":"CREDIT","merchant_id":10,"merchant_name":"Test","transaction_time":"2026-06-26T10:00:00Z"}],"totalRecords":1}
        """;
    when(redis.get("transactions:all:p:1:s:10:k:")).thenReturn(Future.succeededFuture(json));
    when(repo.findAllTransactionMerchant(any())).thenReturn(Future.succeededFuture(new PagedResult<>(java.util.List.of(), 0)));
    when(redis.setJson(any(), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));

    var req = FindAllMerchantTransaction.newBuilder().setPage(1).setPageSize(10).build();
    service.getTransactions(req)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res).isNotNull();
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("getTransactions fetches from repo on cache miss")
  void getTransactionsCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var txn = aTxn(100);
    var paged = new PagedResult<>(List.of(txn), 1);

    when(redis.get("transactions:all:p:1:s:10:k:")).thenReturn(Future.succeededFuture(null));
    when(repo.findAllTransactionMerchant(any())).thenReturn(Future.succeededFuture(paged));
    when(redis.setJson(any(), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));

    var req = FindAllMerchantTransaction.newBuilder().setPage(1).setPageSize(10).build();
    service.getTransactions(req)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res.getData()).hasSize(1);
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("getTransactionsByApiKey fetches from repo on cache miss")
  void getTransactionsByApiKeySuccess(VertxTestContext ctx) {
    mockTracing();
    var txn = aTxn(100);
    var paged = new PagedResult<>(List.of(txn), 1);

    when(redis.get("transactions:apikey:key_abc:p:1:s:10:k:")).thenReturn(Future.succeededFuture(null));
    when(repo.findAllTransactionByApikey(any())).thenReturn(Future.succeededFuture(paged));
    when(redis.setJson(any(), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));

    var req = FindAllMerchantTransactionApikey.newBuilder().setApiKey("key_abc").setPage(1).setPageSize(10).build();
    service.getTransactionsByApiKey(req)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res.getData()).hasSize(1);
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("getTransactionsByMerchantId fetches from repo on cache miss")
  void getTransactionsByMerchantIdSuccess(VertxTestContext ctx) {
    mockTracing();
    var txn = aTxn(100);
    var paged = new PagedResult<>(List.of(txn), 1);

    when(redis.get("transactions:merchant:10:p:1:s:10:k:")).thenReturn(Future.succeededFuture(null));
    when(repo.findAllTransactionByMerchant(any())).thenReturn(Future.succeededFuture(paged));
    when(redis.setJson(any(), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));

    var req = FindAllMerchantTransactionId.newBuilder().setId(10).setPage(1).setPageSize(10).build();
    service.getTransactionsByMerchantId(req)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res.getData()).hasSize(1);
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }
}
