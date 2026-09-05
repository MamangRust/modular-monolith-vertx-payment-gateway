package io.example.merchant.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.example.merchant.model.Merchant;
import io.example.merchant.model.MerchantDocument;
import io.example.merchant.repository.MerchantDocumentCommandRepository;
import io.example.merchant.repository.MerchantDocumentQueryRepository;
import io.example.merchant.repository.MerchantQueryRepository;
import io.example.merchant.repository.UserClientRepository;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import pb.merchant_document.MerchantDocumentCommand.CreateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest;
import pb.user.User.UserResponse;

@ExtendWith({ MockitoExtension.class, VertxExtension.class })
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class MerchantDocumentCommandServiceImplTest {

  @Mock
  private MerchantDocumentCommandRepository repo;

  @Mock
  private MerchantDocumentQueryRepository queryRepository;

  @Mock
  private MerchantQueryRepository merchantQueryRepo;

  @Mock
  private UserClientRepository userClientRepo;

  @Mock
  private RedisService redisService;

  @Mock
  private KafkaService kafkaService;

  @Mock
  private TracingMetrics tracingMetrics;

  private MerchantDocumentCommandServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new MerchantDocumentCommandServiceImpl(
        repo, queryRepository, merchantQueryRepo, userClientRepo, redisService, kafkaService, tracingMetrics);
  }

  private void mockTracing() {
    var tc = new TracingContext(io.opentelemetry.context.Context.root(), java.time.Instant.now());
    when(tracingMetrics.startSpan(any(String.class))).thenReturn(tc);
    when(tracingMetrics.startSpan(any(String.class), any())).thenReturn(tc);
  }

  private static Merchant aMerchant(int id, int userId) {
    return Merchant.builder()
        .id(id).name("Test Merchant").apiKey("key_" + id).userId(userId).status("active")
        .createdAt(Timestamp.from(Instant.now())).updatedAt(Timestamp.from(Instant.now())).build();
  }

  private static MerchantDocument aDoc(int id, int merchantId) {
    return MerchantDocument.builder()
        .id(id).merchantId(merchantId).documentType("ID_CARD").documentUrl("http://doc")
        .status("pending").note("").createdAt(Timestamp.from(Instant.now()))
        .updatedAt(Timestamp.from(Instant.now())).build();
  }

  private static UserResponse aUserResponse(int id, String email) {
    return UserResponse.newBuilder()
        .setId(id).setEmail(email).setFirstname("John").setLastname("Doe").build();
  }

  @Test
  @DisplayName("createMerchantDocument success")
  void createSuccess(VertxTestContext ctx) {
    mockTracing();
    var req = CreateMerchantDocumentRequest.newBuilder()
        .setMerchantId(10).setDocumentType("ID_CARD").setDocumentUrl("http://doc").build();
    var merchant = aMerchant(10, 42);
    var user = aUserResponse(42, "user@example.com");
    var doc = aDoc(100, 10);

    when(merchantQueryRepo.findByMerchantId(10)).thenReturn(Future.succeededFuture(merchant));
    when(userClientRepo.getUserById(42)).thenReturn(Future.succeededFuture(user));
    when(repo.createMerchantDocument(req)).thenReturn(Future.succeededFuture(doc));
    when(
        kafkaService.sendMessage(eq("email-service-topic-merchant-document-create"), eq("100"), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture());

    service.createMerchantDocument(req)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res.getId()).isEqualTo(100);
          assertThat(res.getDocumentType()).isEqualTo("ID_CARD");
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("createMerchantDocument merchant not found")
  void createMerchantNotFound(VertxTestContext ctx) {
    mockTracing();
    var req = CreateMerchantDocumentRequest.newBuilder().setMerchantId(99).build();
    when(merchantQueryRepo.findByMerchantId(99)).thenReturn(Future.succeededFuture(null));

    service.createMerchantDocument(req)
        .onSuccess(res -> ctx.failNow(new AssertionError("Expected failure")))
        .onFailure(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        }));
  }

  @Test
  @DisplayName("updateMerchantDocument success")
  void updateSuccess(VertxTestContext ctx) {
    mockTracing();
    var req = UpdateMerchantDocumentRequest.newBuilder()
        .setDocumentId(100).setDocumentType("PASSPORT").build();
    var doc = aDoc(100, 10);
    doc.setDocumentType("PASSPORT");

    when(repo.updateMerchantDocument(req)).thenReturn(Future.succeededFuture(doc));
    when(redisService.delete("merchant_document:100")).thenReturn(Future.succeededFuture(1L));

    service.updateMerchantDocument(req)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res.getDocumentType()).isEqualTo("PASSPORT");
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("updateMerchantDocument not found")
  void updateNotFound(VertxTestContext ctx) {
    mockTracing();
    var req = UpdateMerchantDocumentRequest.newBuilder().setDocumentId(999).build();
    when(repo.updateMerchantDocument(req)).thenReturn(Future.succeededFuture(null));

    service.updateMerchantDocument(req)
        .onSuccess(res -> ctx.failNow(new AssertionError("Expected failure")))
        .onFailure(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class);
          ctx.completeNow();
        }));
  }

  @Test
  @DisplayName("updateMerchantDocumentStatus approved")
  void updateStatusApproved(VertxTestContext ctx) {
    mockTracing();
    var req = UpdateMerchantDocumentStatusRequest.newBuilder()
        .setMerchantId(10).setDocumentId(100).setStatus("approved").setNote("Clear").build();
    var merchant = aMerchant(10, 42);
    var user = aUserResponse(42, "user@example.com");
    var doc = aDoc(100, 10);
    doc.setStatus("approved");

    when(merchantQueryRepo.findByMerchantId(10)).thenReturn(Future.succeededFuture(merchant));
    when(userClientRepo.getUserById(42)).thenReturn(Future.succeededFuture(user));
    when(repo.updateMerchantDocumentStatus(req)).thenReturn(Future.succeededFuture(doc));
    when(kafkaService.sendMessage(eq("email-service-topic-merchant-document-update-status"), eq("10"),
        any(JsonObject.class)))
        .thenReturn(Future.succeededFuture());
    when(redisService.delete("merchant_document:100")).thenReturn(Future.succeededFuture(1L));

    service.updateMerchantDocumentStatus(req)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("approved");
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("trashedMerchantDocument success")
  void trashedSuccess(VertxTestContext ctx) {
    mockTracing();
    var doc = aDoc(100, 10);
    doc.setDeletedAt(Timestamp.from(Instant.now()));

    when(repo.trashedMerchantDocument(100)).thenReturn(Future.succeededFuture(doc));
    when(redisService.delete("merchant_document:100")).thenReturn(Future.succeededFuture(1L));

    service.trashedMerchantDocument(100)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res.getDeletedAt()).isNotNull();
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("restoreMerchantDocument success")
  void restoreSuccess(VertxTestContext ctx) {
    mockTracing();
    var trashed = aDoc(100, 10);
    trashed.setDeletedAt(Timestamp.from(Instant.now()));
    var restored = aDoc(100, 10);

    when(queryRepository.findByTrashedByIdDocument(100)).thenReturn(Future.succeededFuture(trashed));
    when(repo.restoreMerchantDocument(100)).thenReturn(Future.succeededFuture(restored));
    when(redisService.delete("merchant_document:100")).thenReturn(Future.succeededFuture(1L));

    service.restoreMerchantDocument(100)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res.getId()).isEqualTo(100);
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("deleteMerchantDocumentPermanent success")
  void deletePermanentSuccess(VertxTestContext ctx) {
    mockTracing();
    var trashed = aDoc(100, 10);
    trashed.setDeletedAt(Timestamp.from(Instant.now()));

    when(queryRepository.findByTrashedByIdDocument(100)).thenReturn(Future.succeededFuture(trashed));
    when(repo.deleteMerchantDocumentPermanent(100)).thenReturn(Future.succeededFuture(true));
    when(redisService.delete("merchant_document:100")).thenReturn(Future.succeededFuture(1L));

    service.deleteMerchantDocumentPermanent(100)
        .onSuccess(res -> ctx.verify(ctx::completeNow))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("restoreAllMerchantDocument success")
  void restoreAllSuccess(VertxTestContext ctx) {
    mockTracing();
    when(repo.restoreAllMerchantDocuments()).thenReturn(Future.succeededFuture(3));
    when(redisService.delete("merchant_document:list")).thenReturn(Future.succeededFuture(1L));
    when(redisService.delete("merchant_document:list:trashed")).thenReturn(Future.succeededFuture(1L));

    service.restoreAllMerchantDocument()
        .onSuccess(res -> ctx.verify(ctx::completeNow))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("deleteAllMerchantDocumentPermanent success")
  void deleteAllPermanentSuccess(VertxTestContext ctx) {
    mockTracing();
    when(repo.deleteAllMerchantDocumentsPermanent()).thenReturn(Future.succeededFuture(2));
    when(redisService.delete("merchant_document:list")).thenReturn(Future.succeededFuture(1L));
    when(redisService.delete("merchant_document:list:trashed")).thenReturn(Future.succeededFuture(1L));

    service.deleteAllMerchantDocumentPermanent()
        .onSuccess(res -> ctx.verify(ctx::completeNow))
        .onFailure(ctx::failNow);
  }
}
