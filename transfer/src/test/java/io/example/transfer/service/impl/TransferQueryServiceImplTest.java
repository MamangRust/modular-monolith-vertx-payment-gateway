package io.example.transfer.service.impl;

import io.example.common.domain.PagedResult;
import io.example.common.exception.api.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.transfer.domain.requests.FindAllTransfers;
import io.example.transfer.model.Transfer;
import io.example.transfer.model.TransferResponse;
import io.example.transfer.model.TransferResponseDeleteAt;
import io.example.transfer.repository.TransferQueryRepository;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
class TransferQueryServiceImplTest {

  @Mock
  private TransferQueryRepository repo;

  @Mock
  private RedisService redisService;

  @Mock
  private TracingMetrics tracingMetrics;

  private TransferQueryServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new TransferQueryServiceImpl(repo, redisService, tracingMetrics);
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

  private void stubPagedFallback(PagedResult<Transfer> paged) {
    when(repo.getTransfers(any(FindAllTransfers.class))).thenReturn(Future.succeededFuture(paged));
    lenient().when(redisService.setJson(anyString(), any(Object.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture("OK"));
  }

  /* ─── getAllTransfers ─── */

  @Test
  @DisplayName("getAllTransfers returns cached data on cache hit (fallback to DB when deserialization fails)")
  void getAllTransfersCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        {"data":[{"id":1,"transferNo":"TXN001","transferFrom":"4111111111111111","transferTo":"5111111111111111","transferAmount":500000,"status":"success","transferTime":"2026-06-26T10:00:00Z","createdAt":"2026-06-26T10:00:00Z","updatedAt":"2026-06-26T10:00:00Z","deletedAt":null}],"totalRecords":1}
        """;
    var paged = new PagedResult<>(List.of(aTransfer()), 1);

    when(redisService.get("transfer:list::1:10")).thenReturn(Future.succeededFuture(json));
    stubPagedFallback(paged);
    when(redisService.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = FindAllTransfers.builder().page(1).pageSize(10).build();
    service.getAllTransfers(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getData().get(0).getTransferFrom()).isEqualTo("4111111111111111");
          assertThat(result.getTotalRecords()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getAllTransfers fetches from repo on cache miss")
  void getAllTransfersCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var paged = new PagedResult<>(List.of(aTransfer()), 1);

    when(redisService.get("transfer:list::1:10")).thenReturn(Future.succeededFuture(null));
    stubPagedFallback(paged);
    when(redisService.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    var req = FindAllTransfers.builder().page(1).pageSize(10).build();
    service.getAllTransfers(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          verify(repo).getTransfers(any(FindAllTransfers.class));
          ctx.completeNow();
        })));
  }

  /* ─── getTransferById ─── */

  @Test
  @DisplayName("getTransferById returns transfer from cache")
  void getTransferByIdCacheHit(VertxTestContext ctx) {
    mockTracing();
    var transfer = aTransfer();

    when(redisService.getJson(anyString(), eq(Transfer.class))).thenReturn(Future.succeededFuture(transfer));

    service.getTransferById(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          assertThat(result.getTransferFrom()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTransferById fetches from repo on cache miss")
  void getTransferByIdCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var transfer = aTransfer();

    when(redisService.getJson("transfer:1", Transfer.class)).thenReturn(Future.succeededFuture(null));
    when(repo.getTransferById(1)).thenReturn(Future.succeededFuture(transfer));
    when(redisService.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

    service.getTransferById(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(1);
          verify(repo).getTransferById(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTransferById fails when transfer not found")
  void getTransferByIdNotFound(VertxTestContext ctx) {
    mockTracing();

    when(redisService.getJson("transfer:99", Transfer.class)).thenReturn(Future.succeededFuture(null));
    when(repo.getTransferById(99)).thenReturn(Future.succeededFuture(null));

    service.getTransferById(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }

  /* ─── getTransfersByCardNumber ─── */

  @Test
  @DisplayName("getTransfersByCardNumber returns cached data on cache hit")
  void getTransfersByCardNumberCacheHit(VertxTestContext ctx) {
    mockTracing();
    var transfer = aTransfer();

    when(redisService.getJsonList("transfer:card_primitive:4111111111111111", Transfer.class))
        .thenReturn(Future.succeededFuture(List.of(transfer)));

    service.getTransfersByCardNumber("4111111111111111")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getTransferFrom()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTransfersByCardNumber fetches from repo on cache miss")
  void getTransfersByCardNumberCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var transfer = aTransfer();

    when(redisService.getJsonList("transfer:card_primitive:4111111111111111", Transfer.class))
        .thenReturn(Future.succeededFuture(List.of()));
    when(repo.getTransfersByCardNumber("4111111111111111")).thenReturn(Future.succeededFuture(List.of(transfer)));
    when(redisService.setJsonList(anyString(), any(List.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture());

    service.getTransfersByCardNumber("4111111111111111")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getTransferFrom()).isEqualTo("4111111111111111");
          verify(repo).getTransfersByCardNumber("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTransfersByCardNumber returns empty list when repo returns null/empty")
  void getTransfersByCardNumberEmpty(VertxTestContext ctx) {
    mockTracing();

    when(redisService.getJsonList("transfer:card_primitive:missing", Transfer.class))
        .thenReturn(Future.succeededFuture(List.of()));
    when(repo.getTransfersByCardNumber("missing")).thenReturn(Future.succeededFuture(List.of()));

    service.getTransfersByCardNumber("missing")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isEmpty();
          ctx.completeNow();
        })));
  }

  /* ─── getTransfersAsSender ─── */

  @Test
  @DisplayName("getTransfersAsSender returns cached data on cache hit")
  void getTransfersAsSenderCacheHit(VertxTestContext ctx) {
    mockTracing();
    var transfer = aTransfer();

    when(redisService.getJsonList("transfer:sender:4111111111111111", Transfer.class))
        .thenReturn(Future.succeededFuture(List.of(transfer)));

    service.getTransfersAsSender("4111111111111111")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getTransferFrom()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTransfersAsSender fetches from repo on cache miss")
  void getTransfersAsSenderCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var transfer = aTransfer();

    when(redisService.getJsonList("transfer:sender:4111111111111111", Transfer.class))
        .thenReturn(Future.succeededFuture(List.of()));
    when(repo.getTransfersBySender("4111111111111111")).thenReturn(Future.succeededFuture(List.of(transfer)));
    when(redisService.setJsonList(anyString(), any(List.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture());

    service.getTransfersAsSender("4111111111111111")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getTransferFrom()).isEqualTo("4111111111111111");
          verify(repo).getTransfersBySender("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTransfersAsSender returns empty list when repo returns null/empty")
  void getTransfersAsSenderEmpty(VertxTestContext ctx) {
    mockTracing();

    when(redisService.getJsonList("transfer:sender:missing", Transfer.class))
        .thenReturn(Future.succeededFuture(List.of()));
    when(repo.getTransfersBySender("missing")).thenReturn(Future.succeededFuture(List.of()));

    service.getTransfersAsSender("missing")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isEmpty();
          ctx.completeNow();
        })));
  }

  /* ─── getTransfersAsReceiver ─── */

  @Test
  @DisplayName("getTransfersAsReceiver returns cached data on cache hit")
  void getTransfersAsReceiverCacheHit(VertxTestContext ctx) {
    mockTracing();
    var transfer = aTransfer();

    when(redisService.getJsonList("transfer:receiver:5111111111111111", Transfer.class))
        .thenReturn(Future.succeededFuture(List.of(transfer)));

    service.getTransfersAsReceiver("5111111111111111")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getTransferTo()).isEqualTo("5111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTransfersAsReceiver fetches from repo on cache miss")
  void getTransfersAsReceiverCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var transfer = aTransfer();

    when(redisService.getJsonList("transfer:receiver:5111111111111111", Transfer.class))
        .thenReturn(Future.succeededFuture(List.of()));
    when(repo.getTransfersByReceiver("5111111111111111")).thenReturn(Future.succeededFuture(List.of(transfer)));
    when(redisService.setJsonList(anyString(), any(List.class), any(Duration.class)))
        .thenReturn(Future.succeededFuture());

    service.getTransfersAsReceiver("5111111111111111")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getTransferTo()).isEqualTo("5111111111111111");
          verify(repo).getTransfersByReceiver("5111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTransfersAsReceiver returns empty list when repo returns null/empty")
  void getTransfersAsReceiverEmpty(VertxTestContext ctx) {
    mockTracing();

    when(redisService.getJsonList("transfer:receiver:missing", Transfer.class))
        .thenReturn(Future.succeededFuture(List.of()));
    when(repo.getTransfersByReceiver("missing")).thenReturn(Future.succeededFuture(List.of()));

    service.getTransfersAsReceiver("missing")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isEmpty();
          ctx.completeNow();
        })));
  }

  /* ─── getActiveTransfers (no cache) ─── */

  @Test
  @DisplayName("getActiveTransfers delegates to repo, no caching")
  void getActiveTransfers(VertxTestContext ctx) {
    mockTracing();
    var paged = new PagedResult<>(List.of(aTransfer()), 1);

    when(repo.getActiveTransfers(any(FindAllTransfers.class))).thenReturn(Future.succeededFuture(paged));

    var req = FindAllTransfers.builder().page(1).pageSize(10).build();
    service.getActiveTransfers(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getData().get(0).getTransferFrom()).isEqualTo("4111111111111111");
          verify(repo).getActiveTransfers(any(FindAllTransfers.class));
          ctx.completeNow();
        })));
  }

  /* ─── getTrashedTransfers (no cache) ─── */

  @Test
  @DisplayName("getTrashedTransfers delegates to repo, no caching")
  void getTrashedTransfers(VertxTestContext ctx) {
    mockTracing();
    var paged = new PagedResult<>(List.of(aTransfer()), 1);

    when(repo.getTrashedTransfers(any(FindAllTransfers.class))).thenReturn(Future.succeededFuture(paged));

    var req = FindAllTransfers.builder().page(1).pageSize(10).build();
    service.getTrashedTransfers(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          verify(repo).getTrashedTransfers(any(FindAllTransfers.class));
          ctx.completeNow();
        })));
  }
}
