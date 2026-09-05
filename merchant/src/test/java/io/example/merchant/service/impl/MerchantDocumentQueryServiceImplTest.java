package io.example.merchant.service.impl;

import io.example.common.domain.PagedResult;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.merchant.model.MerchantDocument;
import io.example.merchant.repository.MerchantDocumentQueryRepository;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.merchant_document.MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class MerchantDocumentQueryServiceImplTest {

  @Mock
  private MerchantDocumentQueryRepository repo;

  @Mock
  private RedisService redisService;

  @Mock
  private TracingMetrics tracingMetrics;

  private MerchantDocumentQueryServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new MerchantDocumentQueryServiceImpl(repo, redisService, tracingMetrics);
  }

  private void mockTracing() {
    var tc = new TracingContext(io.opentelemetry.context.Context.root(), java.time.Instant.now());
    when(tracingMetrics.startSpan(any(String.class))).thenReturn(tc);
    when(tracingMetrics.startSpan(any(String.class), any())).thenReturn(tc);
  }

  private static MerchantDocument aDoc(int id, int merchantId) {
    return MerchantDocument.builder()
        .id(id).merchantId(merchantId).documentType("ID_CARD").documentUrl("http://doc")
        .status("pending").note("").createdAt(Timestamp.from(Instant.now()))
        .updatedAt(Timestamp.from(Instant.now())).build();
  }

  @Test
  @DisplayName("findAll fetches from cache hit")
  void findAllCacheHit(VertxTestContext ctx) {
    mockTracing();
    var json = """
        {"data":[{"id":100,"merchantId":10,"documentType":"ID_CARD","documentUrl":"http://doc","status":"pending","note":"","createdAt":"2026-06-26T10:00:00Z","updatedAt":"2026-06-26T10:00:00Z","deletedAt":null}],"totalRecords":1}
        """;
    when(redisService.get("merchant_document:all:p:1:s:10")).thenReturn(Future.succeededFuture(json));
    when(repo.findAllDocuments(any())).thenReturn(Future.succeededFuture(new PagedResult<>(java.util.List.of(), 0)));
    when(redisService.setJson(any(), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));

    var req = FindAllMerchantDocumentsRequest.newBuilder().setPage(1).setPageSize(10).build();
    service.findAll(req)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res).isNotNull();
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("findAll fetches from repo on cache miss")
  void findAllCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var doc = aDoc(100, 10);
    var paged = new PagedResult<>(List.of(doc), 1);

    when(redisService.get("merchant_document:all:p:1:s:10")).thenReturn(Future.succeededFuture(null));
    when(repo.findAllDocuments(any())).thenReturn(Future.succeededFuture(paged));
    when(redisService.setJson(any(), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));

    var req = FindAllMerchantDocumentsRequest.newBuilder().setPage(1).setPageSize(10).build();
    service.findAll(req)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res.getData()).hasSize(1);
          assertThat(res.getData().get(0).getId()).isEqualTo(100);
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("findByActive fetches from repo on cache miss")
  void findByActiveSuccess(VertxTestContext ctx) {
    mockTracing();
    var doc = aDoc(100, 10);
    var paged = new PagedResult<>(List.of(doc), 1);

    when(redisService.get("merchant_document:active:p:1:s:10")).thenReturn(Future.succeededFuture(null));
    when(repo.findByActiveDocuments(any())).thenReturn(Future.succeededFuture(paged));
    when(redisService.setJson(any(), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));

    var req = FindAllMerchantDocumentsRequest.newBuilder().setPage(1).setPageSize(10).build();
    service.findByActive(req)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res.getData()).hasSize(1);
          assertThat(res.getData().get(0).getId()).isEqualTo(100);
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("findByTrashed fetches from repo on cache miss")
  void findByTrashedSuccess(VertxTestContext ctx) {
    mockTracing();
    var doc = aDoc(100, 10);
    doc.setDeletedAt(Timestamp.from(Instant.now()));
    var paged = new PagedResult<>(List.of(doc), 1);

    when(redisService.get("merchant_document:trashed:p:1:s:10")).thenReturn(Future.succeededFuture(null));
    when(repo.findByTrashedDocuments(any())).thenReturn(Future.succeededFuture(paged));
    when(redisService.setJson(any(), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));

    var req = FindAllMerchantDocumentsRequest.newBuilder().setPage(1).setPageSize(10).build();
    service.findByTrashed(req)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res.getData()).hasSize(1);
          assertThat(res.getData().get(0).getId()).isEqualTo(100);
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("findById document not found")
  void findByIdNotFound(VertxTestContext ctx) {
    mockTracing();
    when(redisService.get("merchant_document:id:999")).thenReturn(Future.succeededFuture(null));
    when(repo.findByIdDocument(999)).thenReturn(Future.succeededFuture(null));

    service.findById(999)
        .onSuccess(res -> ctx.verify(() -> {
          // Should not succeed
          ctx.failNow(new AssertionError("Expected failure"));
        }))
        .onFailure(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        }));
  }
}
