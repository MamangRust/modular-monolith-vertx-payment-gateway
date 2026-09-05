package io.example.transaction.service.impl;

import io.example.common.domain.ApiResponse;
import io.example.common.domain.ApiResponsePagination;
import io.example.common.domain.PagedResult;
import io.example.common.domain.PaginationMeta;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.transaction.domain.requests.FindAllTransactionCardNumber;
import io.example.transaction.domain.requests.FindAllTransactions;
import io.example.transaction.model.Transaction;
import io.example.transaction.model.TransactionResponse;
import io.example.transaction.model.TransactionResponseDeleteAt;
import io.example.transaction.repository.TransactionQueryRepository;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
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
import pb.transaction.TransactionQuery.FindAllTransactionCardNumberRequest;
import pb.transaction.TransactionQuery.FindAllTransactionRequest;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionQueryServiceImplTest {

  @Mock
  private TransactionQueryRepository repo;

  @Mock
  private RedisService redisService;

  @Mock
  private TracingMetrics tracingMetrics;

  private TransactionQueryServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new TransactionQueryServiceImpl(repo, redisService, tracingMetrics);
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

  private String pagedJson() {
    return new JsonObject()
        .put("data", new JsonArray()
            .add(new JsonObject()
                .put("id", 1)
                .put("cardNumber", "4111111111111111")
                .put("amount", 500_000)
                .put("paymentMethod", "BANK")
                .put("merchantId", 1)
                .put("status", "success")
                .put("transactionTime", now().toInstant().toString())
                .put("createdAt", now().toInstant().toString())
                .put("updatedAt", now().toInstant().toString())))
        .put("pagination", new JsonObject()
            .put("currentPage", 1)
            .put("pageSize", 10)
            .put("totalPages", 1)
            .put("totalRecords", 1))
        .encode();
  }

  private PagedResult<Transaction> aPagedResult() {
    return new PagedResult<>(List.of(aTransaction()), 1);
  }

  private void stubPagedRepo() {
    when(repo.getTransactions(any(FindAllTransactions.class))).thenReturn(Future.succeededFuture(aPagedResult()));
    when(repo.getTransactionsByCardNumber(any(FindAllTransactionCardNumber.class)))
        .thenReturn(Future.succeededFuture(aPagedResult()));
  }

  /* ─── getTransactions ─── */

  @Test
  @DisplayName("getTransactions returns cached data on cache hit")
  void getTransactionsCacheHit(VertxTestContext ctx) {
    mockTracing();
    when(redisService.get("transaction:all:1:10:")).thenReturn(Future.succeededFuture(pagedJson()));
    stubPagedRepo();
    lenient().when(redisService.setJson(anyString(), any(JsonObject.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture("OK"));

    var req = FindAllTransactionRequest.newBuilder().setPage(1).setPageSize(10).setSearch("").build();
    service.getTransactions(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.data()).hasSize(1);
          assertThat(result.data().get(0).getCardNumber()).isEqualTo("4111111111111111");
          assertThat(result.status()).isEqualTo("success");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTransactions fetches from repo on cache miss")
  void getTransactionsCacheMiss(VertxTestContext ctx) {
    mockTracing();
    when(redisService.get("transaction:all:1:10:")).thenReturn(Future.succeededFuture(null));
    stubPagedRepo();
    when(redisService.setJson(anyString(), any(JsonObject.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture("OK"));

    var req = FindAllTransactionRequest.newBuilder().setPage(1).setPageSize(10).setSearch("").build();
    service.getTransactions(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.data()).hasSize(1);
          assertThat(result.data().get(0).getCardNumber()).isEqualTo("4111111111111111");
          verify(repo).getTransactions(any(FindAllTransactions.class));
          ctx.completeNow();
        })));
  }

  /* ─── getActiveTransactions ─── */

  @Test
  @DisplayName("getActiveTransactions returns cached data on cache hit")
  void getActiveTransactionsCacheHit(VertxTestContext ctx) {
    mockTracing();
    when(redisService.get("transaction:active:1:10:")).thenReturn(Future.succeededFuture(pagedJson()));
    lenient().when(repo.getActiveTransactions(any(FindAllTransactions.class)))
        .thenReturn(Future.succeededFuture(aPagedResult()));
    lenient().when(redisService.setJson(anyString(), any(JsonObject.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture("OK"));

    var req = FindAllTransactionRequest.newBuilder().setPage(1).setPageSize(10).setSearch("").build();
    service.getActiveTransactions(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.data()).hasSize(1);
          assertThat(result.data().get(0).getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getActiveTransactions fetches from repo on cache miss")
  void getActiveTransactionsCacheMiss(VertxTestContext ctx) {
    mockTracing();
    when(redisService.get("transaction:active:1:10:")).thenReturn(Future.succeededFuture(null));
    when(repo.getActiveTransactions(any(FindAllTransactions.class))).thenReturn(Future.succeededFuture(aPagedResult()));
    when(redisService.setJson(anyString(), any(JsonObject.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture("OK"));

    var req = FindAllTransactionRequest.newBuilder().setPage(1).setPageSize(10).setSearch("").build();
    service.getActiveTransactions(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.data()).hasSize(1);
          verify(repo).getActiveTransactions(any(FindAllTransactions.class));
          ctx.completeNow();
        })));
  }

  /* ─── getTrashedTransactions ─── */

  @Test
  @DisplayName("getTrashedTransactions returns cached data on cache hit")
  void getTrashedTransactionsCacheHit(VertxTestContext ctx) {
    mockTracing();
    when(redisService.get("transaction:trashed:1:10:")).thenReturn(Future.succeededFuture(pagedJson()));
    lenient().when(repo.getTrashedTransactions(any(FindAllTransactions.class)))
        .thenReturn(Future.succeededFuture(aPagedResult()));
    lenient().when(redisService.setJson(anyString(), any(JsonObject.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture("OK"));

    var req = FindAllTransactionRequest.newBuilder().setPage(1).setPageSize(10).setSearch("").build();
    service.getTrashedTransactions(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.data()).hasSize(1);
          assertThat(result.data().get(0).getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTrashedTransactions fetches from repo on cache miss")
  void getTrashedTransactionsCacheMiss(VertxTestContext ctx) {
    mockTracing();
    when(redisService.get("transaction:trashed:1:10:")).thenReturn(Future.succeededFuture(null));
    when(repo.getTrashedTransactions(any(FindAllTransactions.class))).thenReturn(Future.succeededFuture(aPagedResult()));
    when(redisService.setJson(anyString(), any(JsonObject.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture("OK"));

    var req = FindAllTransactionRequest.newBuilder().setPage(1).setPageSize(10).setSearch("").build();
    service.getTrashedTransactions(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.data()).hasSize(1);
          verify(repo).getTrashedTransactions(any(FindAllTransactions.class));
          ctx.completeNow();
        })));
  }

  /* ─── getTransactionById ─── */

  @Test
  @DisplayName("getTransactionById returns transaction from cache")
  void getTransactionByIdCacheHit(VertxTestContext ctx) {
    mockTracing();
    var transaction = aTransaction();
    var cachedJson = transaction.toJson().encode();

    when(redisService.get("transaction:1")).thenReturn(Future.succeededFuture(cachedJson));

    service.getTransactionById(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.status()).isEqualTo("success");
          assertThat(result.data().getId()).isEqualTo(1);
          assertThat(result.data().getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTransactionById fetches from repo on cache miss")
  void getTransactionByIdCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var transaction = aTransaction();

    when(redisService.get("transaction:1")).thenReturn(Future.succeededFuture(null));
    when(repo.getTransactionById(1)).thenReturn(Future.succeededFuture(transaction));
    when(redisService.setJson(anyString(), any(JsonObject.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture("OK"));

    service.getTransactionById(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.status()).isEqualTo("success");
          assertThat(result.data().getId()).isEqualTo(1);
          verify(repo).getTransactionById(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTransactionById returns error when transaction not found")
  void getTransactionByIdNotFound(VertxTestContext ctx) {
    mockTracing();

    when(redisService.get("transaction:99")).thenReturn(Future.succeededFuture(null));
    when(repo.getTransactionById(99)).thenReturn(Future.succeededFuture(null));

    service.getTransactionById(99)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.status()).isEqualTo("error");
          ctx.completeNow();
        })));
  }

  /* ─── getTransactionsByCardNumber ─── */

  @Test
  @DisplayName("getTransactionsByCardNumber returns cached data on cache hit")
  void getTransactionsByCardNumberCacheHit(VertxTestContext ctx) {
    mockTracing();
    var cacheKey = "transaction:card:4111111111111111:1:10:";
    when(redisService.get(cacheKey)).thenReturn(Future.succeededFuture(pagedJson()));
    stubPagedRepo();
    lenient().when(redisService.setJson(anyString(), any(JsonObject.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture("OK"));

    var req = FindAllTransactionCardNumberRequest.newBuilder()
        .setCardNumber("4111111111111111").setPage(1).setPageSize(10).setSearch("").build();
    service.getTransactionsByCardNumber(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.data()).hasSize(1);
          assertThat(result.data().get(0).getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTransactionsByCardNumber fetches from repo on cache miss")
  void getTransactionsByCardNumberCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var cacheKey = "transaction:card:4111111111111111:1:10:";
    when(redisService.get(cacheKey)).thenReturn(Future.succeededFuture(null));
    stubPagedRepo();
    when(redisService.setJson(anyString(), any(JsonObject.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture("OK"));

    var req = FindAllTransactionCardNumberRequest.newBuilder()
        .setCardNumber("4111111111111111").setPage(1).setPageSize(10).setSearch("").build();
    service.getTransactionsByCardNumber(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.data()).hasSize(1);
          verify(repo).getTransactionsByCardNumber(any(FindAllTransactionCardNumber.class));
          ctx.completeNow();
        })));
  }

  /* ─── getTransactionsByMerchantId ─── */

  @Test
  @DisplayName("getTransactionsByMerchantId returns cached data on cache hit")
  void getTransactionsByMerchantIdCacheHit(VertxTestContext ctx) {
    mockTracing();
    var cachedJson = new JsonArray()
        .add(new JsonObject()
            .put("id", 1)
            .put("cardNumber", "4111111111111111")
            .put("amount", 500_000)
            .put("paymentMethod", "BANK")
            .put("merchantId", 1)
            .put("status", "success")
            .put("transactionTime", now().toInstant().toString())
            .put("createdAt", now().toInstant().toString())
            .put("updatedAt", now().toInstant().toString()))
        .encode();

    when(redisService.get("transaction:merchant:1")).thenReturn(Future.succeededFuture(cachedJson));

    service.getTransactionsByMerchantId(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.status()).isEqualTo("success");
          assertThat(result.data()).hasSize(1);
          assertThat(result.data().get(0).getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTransactionsByMerchantId fetches from repo on cache miss")
  void getTransactionsByMerchantIdCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var paged = aPagedResult();

    when(redisService.get("transaction:merchant:1")).thenReturn(Future.succeededFuture(null));
    when(repo.getTransactionsByMerchantId(1)).thenReturn(Future.succeededFuture(paged));
    when(redisService.setJson(anyString(), any(Object.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture("OK"));

    service.getTransactionsByMerchantId(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.status()).isEqualTo("success");
          assertThat(result.data()).hasSize(1);
          verify(repo).getTransactionsByMerchantId(1);
          ctx.completeNow();
        })));
  }
}
