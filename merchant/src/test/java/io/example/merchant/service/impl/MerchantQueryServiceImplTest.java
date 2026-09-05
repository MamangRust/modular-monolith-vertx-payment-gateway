package io.example.merchant.service.impl;

import io.example.common.domain.PagedResult;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.merchant.model.Merchant;
import io.example.merchant.repository.MerchantQueryRepository;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class MerchantQueryServiceImplTest {

  @Mock
  private MerchantQueryRepository repo;

  @Mock
  private RedisService redisService;

  @Mock
  private TracingMetrics tracingMetrics;

  private MerchantQueryServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new MerchantQueryServiceImpl(repo, redisService, tracingMetrics);
  }

  private static Merchant aMerchant(int id, String name) {
    var now = Timestamp.from(Instant.parse("2026-06-26T10:00:00Z"));
    return Merchant.builder()
        .id(id).name(name).apiKey("key_" + id).userId(1).status("active")
        .createdAt(now).updatedAt(now).build();
  }

  private void mockTracing() {
    var tc = new TracingContext(io.opentelemetry.context.Context.root(), java.time.Instant.now());
    when(tracingMetrics.startSpan(any(String.class))).thenReturn(tc);
    when(tracingMetrics.startSpan(any(String.class), any())).thenReturn(tc);
  }

  /* ─── findAll ─── */

  @Test
  @DisplayName("findAll returns cached data on cache hit")
  void findAllCacheHit(VertxTestContext ctx) {
    mockTracing();
    var cachedMerchant = aMerchant(1, "Cached Merchant");
    var cachedPage = new PagedResult<>(List.of(cachedMerchant), 1);
    var json = """
        {"data":[{"id":1,"name":"Cached Merchant","apiKey":"key_1","userId":1,"status":"active","createdAt":"2026-06-26T10:00:00Z","updatedAt":"2026-06-26T10:00:00Z","deletedAt":null}],"totalRecords":1}
        """;

    when(redisService.get("merchant:all:p:1:s:10:k:")).thenReturn(Future.succeededFuture(json));
    when(repo.findAllMerchants(any())).thenReturn(Future.succeededFuture(cachedPage));
    when(redisService.setJson(any(), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));

    var req = pb.merchant.Merchant.FindAllMerchantRequest.newBuilder().setPage(1).setPageSize(10).build();
    service.findAll(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getData().get(0).getName()).isEqualTo("Cached Merchant");
          assertThat(result.getTotalRecords()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findAll fetches from repo on cache miss")
  void findAllCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var merchant = aMerchant(2, "DB Merchant");
    var paged = new PagedResult<>(List.of(merchant), 1);

    when(redisService.get("merchant:all:p:1:s:10:k:")).thenReturn(Future.succeededFuture(null));
    when(repo.findAllMerchants(any())).thenReturn(Future.succeededFuture(paged));
    when(redisService.setJson(any(String.class), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));

    var req = pb.merchant.Merchant.FindAllMerchantRequest.newBuilder().setPage(1).setPageSize(10).build();
    service.findAll(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getData().get(0).getName()).isEqualTo("DB Merchant");
          ctx.completeNow();
        })));
  }

  /* ─── findById ─── */

  @Test
  @DisplayName("findById returns merchant from cache")
  void findByIdCacheHit(VertxTestContext ctx) {
    mockTracing();
    var merchant = aMerchant(5, "Cached By Id");
    var json = """
        {"id":5,"name":"Cached By Id","apiKey":"key_5","userId":1,"status":"active","createdAt":"2026-06-26T10:00:00Z","updatedAt":"2026-06-26T10:00:00Z","deletedAt":null}
        """;

    when(redisService.get("merchant:id:5")).thenReturn(Future.succeededFuture(json));
    when(repo.findByMerchantId(5)).thenReturn(Future.succeededFuture(merchant));
    when(redisService.setJson(any(), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));

    service.findById(5)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(5);
          assertThat(result.getName()).isEqualTo("Cached By Id");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findById fetches from repo on cache miss")
  void findByIdCacheMiss(VertxTestContext ctx) {
    mockTracing();
    var merchant = aMerchant(5, "DB By Id");

    when(redisService.get("merchant:id:5")).thenReturn(Future.succeededFuture(null));
    when(repo.findByMerchantId(5)).thenReturn(Future.succeededFuture(merchant));
    when(redisService.setJson(any(String.class), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));

    service.findById(5)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(5);
          assertThat(result.getName()).isEqualTo("DB By Id");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findById returns failed future when merchant not found")
  void findByIdNotFound(VertxTestContext ctx) {
    mockTracing();
    when(redisService.get("merchant:id:99")).thenReturn(Future.succeededFuture(null));
    when(repo.findByMerchantId(99)).thenReturn(Future.succeededFuture(null));

    service.findById(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }

  /* ─── findByActive ─── */

  @Test
  @DisplayName("findByActive returns paginated active merchants")
  void findByActive(VertxTestContext ctx) {
    mockTracing();
    var merchant = aMerchant(3, "Active Merchant");
    var paged = new PagedResult<>(List.of(merchant), 1);

    when(redisService.get("merchant:active:p:1:s:10:k:")).thenReturn(Future.succeededFuture(null));
    when(repo.findByActive(any())).thenReturn(Future.succeededFuture(paged));
    when(redisService.setJson(any(String.class), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));

    var req = pb.merchant.Merchant.FindAllMerchantRequest.newBuilder().setPage(1).setPageSize(10).build();
    service.findByActive(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getData().get(0).getName()).isEqualTo("Active Merchant");
          ctx.completeNow();
        })));
  }

  /* ─── findByTrashed ─── */

  @Test
  @DisplayName("findByTrashed returns paginated trashed merchants")
  void findByTrashed(VertxTestContext ctx) {
    mockTracing();
    var merchant = aMerchant(4, "Trashed Merchant");
    var paged = new PagedResult<>(List.of(merchant), 1);

    when(redisService.get("merchant:trashed:p:1:s:10:k:")).thenReturn(Future.succeededFuture(null));
    when(repo.findByTrashed(any())).thenReturn(Future.succeededFuture(paged));
    when(redisService.setJson(any(String.class), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));

    var req = pb.merchant.Merchant.FindAllMerchantRequest.newBuilder().setPage(1).setPageSize(10).build();
    service.findByTrashed(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getData().get(0).getName()).isEqualTo("Trashed Merchant");
          ctx.completeNow();
        })));
  }

  /* ─── findByApiKey ─── */

  @Test
  @DisplayName("findByApiKey returns merchant from repo when cache empty")
  void findByApiKey(VertxTestContext ctx) {
    mockTracing();
    var merchant = aMerchant(6, "ApiKey Merchant");

    when(redisService.get("merchant:apikey:key_6")).thenReturn(Future.succeededFuture(null));
    when(repo.findByApiKey("key_6")).thenReturn(Future.succeededFuture(merchant));
    when(redisService.setJson(any(String.class), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));

    service.findByApiKey("key_6")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(6);
          assertThat(result.getName()).isEqualTo("ApiKey Merchant");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByApiKey returns failed future when not found")
  void findByApiKeyNotFound(VertxTestContext ctx) {
    mockTracing();
    when(redisService.get("merchant:apikey:missing")).thenReturn(Future.succeededFuture(null));
    when(repo.findByApiKey("missing")).thenReturn(Future.succeededFuture(null));

    service.findByApiKey("missing")
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }

  /* ─── findByMerchantUserId ─── */

  @Test
  @DisplayName("findByMerchantUserId returns merchants for user")
  void findByMerchantUserId(VertxTestContext ctx) {
    mockTracing();
    var merchant = aMerchant(7, "User Merchant");

    when(redisService.get("merchant:user:1")).thenReturn(Future.succeededFuture(null));
    when(repo.findByMerchantUserId(1)).thenReturn(Future.succeededFuture(List.of(merchant)));
    when(redisService.setJson(any(String.class), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));

    service.findByMerchantUserId(1)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).hasSize(1);
          assertThat(result.get(0).getName()).isEqualTo("User Merchant");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByMerchantUserId returns empty list when no merchants")
  void findByMerchantUserIdEmpty(VertxTestContext ctx) {
    mockTracing();
    when(redisService.get("merchant:user:2")).thenReturn(Future.succeededFuture(null));
    when(repo.findByMerchantUserId(2)).thenReturn(Future.succeededFuture(List.of()));

    service.findByMerchantUserId(2)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isEmpty();
          ctx.completeNow();
        })));
  }
}
