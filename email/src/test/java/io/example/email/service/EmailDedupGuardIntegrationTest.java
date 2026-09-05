package io.example.email.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.example.common.service.RedisService;
import io.example.email.service.EmailDedupGuard.ClaimResult;
import io.opentelemetry.api.OpenTelemetry;
import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Phase 6 — integration test for the idempotency state machine against a real
 * Redis container (no mocks). Covers the test matrix rows "Redis restart" and
 * "Redis down" (fail-open) plus the CLAIMED/BUSY/DUPLICATE transitions that
 * make multi-replica delivery safe.
 */
@Testcontainers(disabledWithoutDocker = true)
class EmailDedupGuardIntegrationTest {

  @Container
  static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

  private static Vertx vertx;
  private static EmailDedupGuard guard;

  @BeforeAll
  static void setUp() {
    vertx = Vertx.vertx();
    RedisAPI redis = RedisAPI.api(Redis.createClient(vertx, new RedisOptions()
        .setType(RedisClientType.STANDALONE)
        .setConnectionString("redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379))));
    guard = new EmailDedupGuard(new RedisService(redis, OpenTelemetry.noop()));
  }

  @AfterAll
  static void tearDown() {
    vertx.close().toCompletionStage().toCompletableFuture().join();
  }

  private static ClaimResult claim(String eventId) {
    return guard.claim(eventId, "email-service-topic-auth-register", 0, 0L)
        .toCompletionStage().toCompletableFuture().join();
  }

  @Test
  void claimTwice_secondIsBusyWhileProcessingLeaseHeld() {
    String eventId = "it-busy-" + System.nanoTime();
    assertThat(claim(eventId)).isEqualTo(ClaimResult.CLAIMED);
    // Second replica attempting the same event while the lease is held → BUSY.
    assertThat(claim(eventId)).isEqualTo(ClaimResult.BUSY);
  }

  @Test
  void markSent_thenClaim_isDuplicate() {
    String eventId = "it-dup-" + System.nanoTime();
    assertThat(claim(eventId)).isEqualTo(ClaimResult.CLAIMED);
    guard.markSent(eventId, "email-service-topic-auth-register", 0, 0L)
        .toCompletionStage().toCompletableFuture().join();
    // Redelivery after a successful send is skipped, not re-sent.
    assertThat(claim(eventId)).isEqualTo(ClaimResult.DUPLICATE);
  }

  @Test
  void release_thenClaim_isClaimedAgain() {
    String eventId = "it-rel-" + System.nanoTime();
    assertThat(claim(eventId)).isEqualTo(ClaimResult.CLAIMED);
    guard.release(eventId, "email-service-topic-auth-register", 0, 0L)
        .toCompletionStage().toCompletableFuture().join();
    // Failed send released the claim → the retry can claim it again.
    assertThat(claim(eventId)).isEqualTo(ClaimResult.CLAIMED);
  }

  @Test
  void redisDown_claimFailsOpen() {
    REDIS.stop();
    try {
      ClaimResult result = claim("it-down-" + System.nanoTime());
      // Fail-open: availability of email notifications wins over exactly-once.
      assertThat(result).isEqualTo(ClaimResult.CLAIMED);
    } finally {
      REDIS.start();
    }
  }
}
