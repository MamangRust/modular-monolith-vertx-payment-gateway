package io.example.merchant.service.impl;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.example.merchant.model.Merchant;
import io.example.merchant.repository.MerchantCommandRepository;
import io.example.merchant.repository.MerchantQueryRepository;
import io.example.merchant.repository.UserClientRepository;
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

import pb.merchant.MerchantCommand.CreateMerchantRequest;
import pb.merchant.MerchantCommand.UpdateMerchantRequest;
import pb.merchant.MerchantCommand.UpdateMerchantStatusRequest;
import pb.user.User;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class MerchantCommandServiceImplTest {

  @Mock
  private MerchantCommandRepository repo;

  @Mock
  private MerchantQueryRepository repoQuery;

  @Mock
  private UserClientRepository userClientRepo;

  @Mock
  private RedisService redisService;

  @Mock
  private KafkaService kafkaService;

  @Mock
  private TracingMetrics tracingMetrics;

  private MerchantCommandServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new MerchantCommandServiceImpl(repo, repoQuery, userClientRepo, redisService, kafkaService, tracingMetrics);
  }

  private void mockTracing() {
    var tc = new TracingContext(io.opentelemetry.context.Context.root(), java.time.Instant.now());
    when(tracingMetrics.startSpan(any(String.class))).thenReturn(tc);
    when(tracingMetrics.startSpan(any(String.class), any())).thenReturn(tc);
  }

  private static Merchant aMerchant(int id, String name, String status) {
    var now = Timestamp.from(Instant.parse("2026-06-26T10:00:00Z"));
    return Merchant.builder()
        .id(id).name(name).apiKey("key_" + id).userId(1).status(status)
        .createdAt(now).updatedAt(now).build();
  }

  private static User.UserResponse aUserResponse(int id, String email) {
    return User.UserResponse.newBuilder()
        .setId(id).setFirstname("Test").setLastname("User").setEmail(email)
        .build();
  }

  /* ─── createMerchant ─── */

  @Test
  @DisplayName("createMerchant creates merchant and sends email")
  void createMerchantSuccess(VertxTestContext ctx) {
    mockTracing();
    var request = CreateMerchantRequest.newBuilder()
        .setName("New Merchant").setUserId(1).build();
    var user = aUserResponse(1, "test@example.com");
    var merchant = aMerchant(10, "New Merchant", "pending");

    when(userClientRepo.getUserById(1)).thenReturn(Future.succeededFuture(user));
    when(repo.createMerchant(request)).thenReturn(Future.succeededFuture(merchant));
    when(kafkaService.sendMessage(eq("email-service-topic-merchant-create"), eq("10"), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture());

    service.createMerchant(request)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(10);
          assertThat(result.getName()).isEqualTo("New Merchant");
          ctx.completeNow();
        })));
  }

  /* ─── updateMerchant ─── */

  @Test
  @DisplayName("updateMerchant updates merchant and evicts cache")
  void updateMerchantSuccess(VertxTestContext ctx) {
    mockTracing();
    var request = UpdateMerchantRequest.newBuilder().setMerchantId(5).setName("Updated").build();
    var merchant = aMerchant(5, "Updated", "active");

    when(repo.updateMerchant(request)).thenReturn(Future.succeededFuture(merchant));
    when(redisService.delete("merchant:5")).thenReturn(Future.succeededFuture(1L));

    service.updateMerchant(request)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getName()).isEqualTo("Updated");
          verify(redisService).delete("merchant:5");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateMerchant fails when merchant not found")
  void updateMerchantNotFound(VertxTestContext ctx) {
    mockTracing();
    var request = UpdateMerchantRequest.newBuilder().setMerchantId(99).setName("Ghost").build();
    when(repo.updateMerchant(request)).thenReturn(Future.succeededFuture(null));

    service.updateMerchant(request)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }

  /* ─── updateMerchantStatus ─── */

  @Test
  @DisplayName("updateMerchantStatus activates merchant and sends email")
  void updateMerchantStatusActive(VertxTestContext ctx) {
    mockTracing();
    var request = UpdateMerchantStatusRequest.newBuilder().setMerchantId(5).setStatus("active").build();
    var merchant = aMerchant(5, "Test", "active");
    var user = aUserResponse(1, "test@example.com");

    when(repoQuery.findByMerchantId(5)).thenReturn(Future.succeededFuture(merchant));
    when(userClientRepo.getUserById(1)).thenReturn(Future.succeededFuture(user));
    when(repo.updateMerchantStatus(request)).thenReturn(Future.succeededFuture(merchant));
    when(kafkaService.sendMessage(eq("email-service-topic-merchant-update-status"), eq("5"), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture());
    when(redisService.delete("merchant:5")).thenReturn(Future.succeededFuture(1L));

    service.updateMerchantStatus(request)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getStatus()).isEqualTo("active");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateMerchantStatus fails when merchant not found")
  void updateMerchantStatusNotFound(VertxTestContext ctx) {
    mockTracing();
    var request = UpdateMerchantStatusRequest.newBuilder().setMerchantId(99).setStatus("active").build();
    when(repoQuery.findByMerchantId(99)).thenReturn(Future.succeededFuture(null));

    service.updateMerchantStatus(request)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }

  /* ─── trashedMerchant ─── */

  @Test
  @DisplayName("trashedMerchant trashes and evicts cache")
  void trashedMerchantSuccess(VertxTestContext ctx) {
    mockTracing();
    var merchant = aMerchant(5, "To Delete", "active");
    when(repo.trashedMerchant(5)).thenReturn(Future.succeededFuture(merchant));
    when(redisService.delete("merchant:5")).thenReturn(Future.succeededFuture(1L));

    service.trashedMerchant(5)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(5);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("trashedMerchant fails when merchant not found")
  void trashedMerchantNotFound(VertxTestContext ctx) {
    mockTracing();
    when(repo.trashedMerchant(99)).thenReturn(Future.succeededFuture(null));

    service.trashedMerchant(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }

  /* ─── restoreMerchant ─── */

  @Test
  @DisplayName("restoreMerchant restores trashed merchant")
  void restoreMerchantSuccess(VertxTestContext ctx) {
    mockTracing();
    var trashed = aMerchant(5, "Trashed", "active");
    var restored = aMerchant(5, "Trashed", "active");

    when(repoQuery.findByTrashedById(5)).thenReturn(Future.succeededFuture(trashed));
    when(repo.restoreMerchant(5)).thenReturn(Future.succeededFuture(restored));
    when(redisService.delete("merchant:5")).thenReturn(Future.succeededFuture(1L));

    service.restoreMerchant(5)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getId()).isEqualTo(5);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("restoreMerchant fails when not trashed")
  void restoreMerchantNotTrashed(VertxTestContext ctx) {
    mockTracing();
    when(repoQuery.findByTrashedById(99)).thenReturn(Future.succeededFuture(null));

    service.restoreMerchant(99)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class);
          ctx.completeNow();
        })));
  }

  /* ─── deleteMerchantPermanent ─── */

  @Test
  @DisplayName("deleteMerchantPermanent deletes trashed merchant")
  void deleteMerchantPermanentSuccess(VertxTestContext ctx) {
    mockTracing();
    var trashed = aMerchant(5, "To Delete", "active");

    when(repoQuery.findByTrashedById(5)).thenReturn(Future.succeededFuture(trashed));
    when(repo.deleteMerchantPermanent(5)).thenReturn(Future.succeededFuture(true));
    when(redisService.delete("merchant:5")).thenReturn(Future.succeededFuture(1L));

    service.deleteMerchantPermanent(5)
        .onComplete(ctx.succeeding(v -> ctx.verify(ctx::completeNow)));
  }

  /* ─── restoreAllMerchant ─── */

  @Test
  @DisplayName("restoreAllMerchant restores all trashed merchants")
  void restoreAllMerchantSuccess(VertxTestContext ctx) {
    mockTracing();
    when(repo.restoreAllMerchants()).thenReturn(Future.succeededFuture(5));
    when(redisService.delete("merchant:list")).thenReturn(Future.succeededFuture(1L));
    when(redisService.delete("merchant:list:trashed")).thenReturn(Future.succeededFuture(1L));

    service.restoreAllMerchant()
        .onComplete(ctx.succeeding(v -> ctx.verify(ctx::completeNow)));
  }

  @Test
  @DisplayName("restoreAllMerchant fails when no trashed merchants")
  void restoreAllMerchantNone(VertxTestContext ctx) {
    mockTracing();
    when(repo.restoreAllMerchants()).thenReturn(Future.succeededFuture(0));

    service.restoreAllMerchant()
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        })));
  }

  /* ─── deleteAllMerchantPermanent ─── */

  @Test
  @DisplayName("deleteAllMerchantPermanent deletes all trashed merchants")
  void deleteAllMerchantPermanentSuccess(VertxTestContext ctx) {
    mockTracing();
    when(repo.deleteAllMerchantsPermanent()).thenReturn(Future.succeededFuture(3));
    when(redisService.delete("merchant:list")).thenReturn(Future.succeededFuture(1L));
    when(redisService.delete("merchant:list:trashed")).thenReturn(Future.succeededFuture(1L));

    service.deleteAllMerchantPermanent()
        .onComplete(ctx.succeeding(v -> ctx.verify(ctx::completeNow)));
  }
}
