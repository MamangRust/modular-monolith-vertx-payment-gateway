package io.example.email.service;

import io.example.common.service.RedisService;
import io.example.email.observability.EmailMetrics;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Guards the email consumer against duplicate deliveries caused by Kafka
 * retries, rebalances, or topic replays.
 *
 * <p>The idempotency key is derived from the event envelope's {@code event_id}
 * (the stable business-event identity), falling back to the Kafka record
 * identity {@code (topic, partition, offset)} for legacy payloads without an
 * envelope. A redelivered record or an outbox replay keeps the same
 * {@code event_id}, so it is skipped; distinct legitimate events get distinct
 * IDs and are both delivered.
 *
 * <p>State machine (matches the shared best-practice baseline):
 *
 * <pre>
 * ABSENT --claim (atomic SET NX EX lease)--> PROCESSING --send ok--> SENT (TTL 24h)
 *                                             |--send fail--> release (DEL) -&gt; retry
 * </pre>
 *
 * <p><b>Fail-open:</b> if Redis is unavailable (or the guard fails for any
 * reason), the message is still delivered — availability of email
 * notifications is preferred over strict exactly-once semantics.
 */
public class EmailDedupGuard {

  private static final Logger log = LoggerFactory.getLogger(EmailDedupGuard.class);

  public enum ClaimResult {
    /** This consumer owns the claim and must send the email. */
    CLAIMED,
    /** The record was already delivered successfully; skip without sending. */
    DUPLICATE,
    /** Another consumer/lease holds the claim; do not send and do not commit. */
    BUSY
  }

  /** Lease on the PROCESSING claim; a crashed worker blocks at most this long. */
  public static final Duration PROCESSING_LEASE = Duration.ofSeconds(60);
  /** How long a successfully delivered event stays deduplicated. */
  public static final Duration SENT_TTL = Duration.ofHours(24);

  private static final String PREFIX = "email:idempotency:";
  private static final String STATE_PROCESSING = "PROCESSING";
  private static final String STATE_SENT = "SENT";

  private final RedisService redisService;
  private final EmailMetrics metrics;

  public EmailDedupGuard(RedisService redisService) {
    this(redisService, EmailMetrics.noop());
  }

  public EmailDedupGuard(RedisService redisService, EmailMetrics metrics) {
    this.redisService = redisService;
    this.metrics = metrics;
  }

  public static String dedupKey(String eventId, String topic, int partition, long offset) {
    return (eventId != null && !eventId.isBlank())
        ? PREFIX + eventId
        : PREFIX + topic + ":" + partition + ":" + offset;
  }

  /**
   * Atomically claims the record. Claiming is a single {@code SET NX EX} so two
   * consumers can never claim the same record at the same time.
   *
   * @return {@link ClaimResult#CLAIMED} when this consumer owns the claim,
   *         {@link ClaimResult#DUPLICATE} when the record was already delivered,
   *         {@link ClaimResult#BUSY} when another worker holds the PROCESSING lease.
   */
  public Future<ClaimResult> claim(String eventId, String topic, int partition, long offset) {
    String key = dedupKey(eventId, topic, partition, offset);

    return redisService.setIfAbsent(key, STATE_PROCESSING, PROCESSING_LEASE)
        .compose(claimed -> {
          if (Boolean.TRUE.equals(claimed)) {
            return Future.succeededFuture(ClaimResult.CLAIMED);
          }
          // Key already exists → inspect its state.
          return redisService.get(key)
              .compose(value -> {
                if (STATE_SENT.equals(value)) {
                  log.info("⏭️ Duplicate Kafka record skipped (already SENT, key: {})", key);
                  return Future.succeededFuture(ClaimResult.DUPLICATE);
                }
                if (STATE_PROCESSING.equals(value)) {
                  return redisService.ttl(key).compose(ttl -> {
                    if (ttl != null && ttl == -1) {
                      // Orphan: PROCESSING key without expiry (defensive — the
                      // atomic SET NX EX makes this unlikely). Reclaim it instead
                      // of blocking the record forever.
                      log.warn("🧹 Reclaiming orphan PROCESSING key without TTL: {}", key);
                      return redisService.delete(key)
                          .compose(v -> redisService.setIfAbsent(key, STATE_PROCESSING, PROCESSING_LEASE))
                          .map(reclaimed -> Boolean.TRUE.equals(reclaimed) ? ClaimResult.CLAIMED : ClaimResult.BUSY);
                    }
                    return Future.succeededFuture(ClaimResult.BUSY);
                  });
                }
                // Unknown/legacy value: do not override; treat as busy (safe).
                return Future.succeededFuture(ClaimResult.BUSY);
              });
        })
        .otherwise(err -> {
          // Fail-open: availability of email notifications > exactly-once. The
          // failure is still counted so the fail-open window is observable.
          metrics.recordClaimFailed();
          log.warn("⚠️ Dedup claim failed for key {}, proceeding to send (fail-open): {}", key, err.getMessage());
          return ClaimResult.CLAIMED;
        });
  }

  /**
   * Marks the record as successfully delivered. Fail-open: if the marker cannot
   * be written the caller still proceeds (the email was already sent).
   */
  public Future<Void> markSent(String eventId, String topic, int partition, long offset) {
    String key = dedupKey(eventId, topic, partition, offset);
    return redisService.set(key, STATE_SENT, SENT_TTL)
        .<Void>mapEmpty()
        .onSuccess(v -> log.debug("Marked SENT: {}", key))
        .otherwise(err -> {
          log.warn("⚠️ Failed to mark SENT for key {}, proceeding (fail-open): {}", key, err.getMessage());
          return null;
        });
  }

  /**
   * Releases the PROCESSING claim after a failed send so the retry can claim
   * the record again. Fail-open: even if the release fails, the lease expiry
   * will eventually free the claim.
   */
  public Future<Void> release(String eventId, String topic, int partition, long offset) {
    String key = dedupKey(eventId, topic, partition, offset);
    return redisService.delete(key)
        .<Void>mapEmpty()
        .onSuccess(v -> log.debug("Released claim: {}", key))
        .otherwise(err -> {
          log.warn("⚠️ Failed to release claim for key {}, proceeding: {}", key, err.getMessage());
          return null;
        });
  }
}
