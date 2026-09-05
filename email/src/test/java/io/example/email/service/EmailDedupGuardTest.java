package io.example.email.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.common.service.RedisService;
import io.example.email.service.EmailDedupGuard.ClaimResult;
import io.vertx.core.Future;

@ExtendWith(MockitoExtension.class)
class EmailDedupGuardTest {

  @Mock
  private RedisService redisService;

  private EmailDedupGuard guard;

  @BeforeEach
  void setUp() {
    guard = new EmailDedupGuard(redisService);
  }

  @Test
  void dedupKey_shouldUseEventIdWhenPresent() {
    assertThat(EmailDedupGuard.dedupKey("evt-123", "email-service-topic-auth-register", 3, 42L))
        .isEqualTo("email:idempotency:evt-123");
  }

  @Test
  void dedupKey_shouldFallBackToRecordIdentityWithoutEventId() {
    assertThat(EmailDedupGuard.dedupKey(null, "email-service-topic-auth-register", 3, 42L))
        .isEqualTo("email:idempotency:email-service-topic-auth-register:3:42");
  }

  @Test
  void claim_shouldReturnClaimedWhenSetNxSucceeds() {
    String key = EmailDedupGuard.dedupKey("evt-123", "email-service-topic-auth-register", 0, 7L);
    when(redisService.setIfAbsent(key, "PROCESSING", EmailDedupGuard.PROCESSING_LEASE))
        .thenReturn(Future.succeededFuture(true));

    Future<ClaimResult> result = guard.claim("evt-123", "email-service-topic-auth-register", 0, 7L);

    assertThat(result.succeeded()).as("Failed: %s", result.cause()).isTrue();
    assertThat(result.result()).isEqualTo(ClaimResult.CLAIMED);
  }

  @Test
  void claim_shouldReturnDuplicateWhenAlreadySent() {
    String key = EmailDedupGuard.dedupKey("evt-123", "email-service-topic-auth-register", 0, 7L);
    when(redisService.setIfAbsent(key, "PROCESSING", EmailDedupGuard.PROCESSING_LEASE))
        .thenReturn(Future.succeededFuture(false));
    when(redisService.get(key)).thenReturn(Future.succeededFuture("SENT"));

    Future<ClaimResult> result = guard.claim("evt-123", "email-service-topic-auth-register", 0, 7L);

    assertThat(result.succeeded()).as("Failed: %s", result.cause()).isTrue();
    assertThat(result.result()).isEqualTo(ClaimResult.DUPLICATE);
  }

  @Test
  void claim_shouldReturnBusyWhenProcessingLeaseActive() {
    String key = EmailDedupGuard.dedupKey("evt-123", "email-service-topic-merchant-create", 1, 3L);
    when(redisService.setIfAbsent(key, "PROCESSING", EmailDedupGuard.PROCESSING_LEASE))
        .thenReturn(Future.succeededFuture(false));
    when(redisService.get(key)).thenReturn(Future.succeededFuture("PROCESSING"));
    when(redisService.ttl(key)).thenReturn(Future.succeededFuture(42L));

    Future<ClaimResult> result = guard.claim("evt-123", "email-service-topic-merchant-create", 1, 3L);

    assertThat(result.succeeded()).as("Failed: %s", result.cause()).isTrue();
    assertThat(result.result()).isEqualTo(ClaimResult.BUSY);
  }

  @Test
  void claim_shouldReclaimOrphanProcessingKeyWithoutTtl() {
    String key = EmailDedupGuard.dedupKey("evt-123", "email-service-topic-transaction-create", 2, 9L);
    when(redisService.setIfAbsent(key, "PROCESSING", EmailDedupGuard.PROCESSING_LEASE))
        .thenReturn(Future.succeededFuture(false), Future.succeededFuture(true));
    when(redisService.get(key)).thenReturn(Future.succeededFuture("PROCESSING"));
    when(redisService.ttl(key)).thenReturn(Future.succeededFuture(-1L));
    when(redisService.delete(key)).thenReturn(Future.succeededFuture(1L));

    Future<ClaimResult> result = guard.claim("evt-123", "email-service-topic-transaction-create", 2, 9L);

    assertThat(result.succeeded()).as("Failed: %s", result.cause()).isTrue();
    assertThat(result.result()).isEqualTo(ClaimResult.CLAIMED);
    verify(redisService).delete(key);
  }

  @Test
  void claim_shouldFailOpenWhenSetNxFails() {
    String key = EmailDedupGuard.dedupKey("evt-123", "email-service-topic-auth-register", 0, 7L);
    when(redisService.setIfAbsent(key, "PROCESSING", EmailDedupGuard.PROCESSING_LEASE))
        .thenReturn(Future.failedFuture(new RuntimeException("redis down")));

    Future<ClaimResult> result = guard.claim("evt-123", "email-service-topic-auth-register", 0, 7L);

    assertThat(result.succeeded()).as("Failed: %s", result.cause()).isTrue();
    assertThat(result.result()).isEqualTo(ClaimResult.CLAIMED);
  }

  @Test
  void claim_shouldFailOpenWhenStateLookupFails() {
    String key = EmailDedupGuard.dedupKey("evt-123", "email-service-topic-auth-register", 0, 7L);
    when(redisService.setIfAbsent(key, "PROCESSING", EmailDedupGuard.PROCESSING_LEASE))
        .thenReturn(Future.succeededFuture(false));
    when(redisService.get(key)).thenReturn(Future.failedFuture(new RuntimeException("redis down")));

    Future<ClaimResult> result = guard.claim("evt-123", "email-service-topic-auth-register", 0, 7L);

    assertThat(result.succeeded()).as("Failed: %s", result.cause()).isTrue();
    assertThat(result.result()).isEqualTo(ClaimResult.CLAIMED);
  }

  @Test
  void markSent_shouldPersistSentStateWithTtl() {
    String key = EmailDedupGuard.dedupKey("evt-123", "email-service-topic-auth-register", 0, 7L);
    when(redisService.set(key, "SENT", EmailDedupGuard.SENT_TTL)).thenReturn(Future.succeededFuture("OK"));

    Future<Void> result = guard.markSent("evt-123", "email-service-topic-auth-register", 0, 7L);

    assertThat(result.succeeded()).as("Failed: %s", result.cause()).isTrue();
    verify(redisService).set(key, "SENT", EmailDedupGuard.SENT_TTL);
  }

  @Test
  void release_shouldDeleteClaimKey() {
    String key = EmailDedupGuard.dedupKey("evt-123", "email-service-topic-auth-register", 0, 7L);
    when(redisService.delete(key)).thenReturn(Future.succeededFuture(1L));

    Future<Void> result = guard.release("evt-123", "email-service-topic-auth-register", 0, 7L);

    assertThat(result.succeeded()).as("Failed: %s", result.cause()).isTrue();
    verify(redisService).delete(key);
  }
}
