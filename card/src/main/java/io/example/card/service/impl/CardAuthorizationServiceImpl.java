package io.example.card.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.card.model.CardAuthTransaction;
import io.example.card.model.CardCreditAccount;
import io.example.card.repository.CardAuthTransactionRepository;
import io.example.card.repository.CardCreditAccountRepository;
import io.example.card.service.CardAuthorizationService;
import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.ConflictException;
import io.example.common.exception.grpc.FailedPreconditionException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.example.common.utils.DeclineCodeMapper;
import io.grpc.StatusRuntimeException;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.UUID;

@RequiredArgsConstructor
public class CardAuthorizationServiceImpl implements CardAuthorizationService {
  private static final Logger log = LoggerFactory.getLogger(CardAuthorizationServiceImpl.class);

  private static final String REDIS_CREDIT_KEY = "card:%s:credit";
  private static final String REDIS_VELOCITY_KEY = "card:%s:velocity:60s";
  private static final Duration REDIS_CACHE_TTL = Duration.ofSeconds(60);
  private static final Duration VELOCITY_WINDOW_TTL = Duration.ofSeconds(60);
  private static final int VELOCITY_LIMIT = 10; // max 10 txns per 60s

  private final CardCreditAccountRepository creditAccountRepo;
  private final CardAuthTransactionRepository authTxnRepo;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private final KafkaService kafkaService;

  @Override
  public Future<CardAuthTransaction> authorize(String cardNumber, Integer merchantId, Long amount,
                                                String currency, String posEntryMode, String mcc,
                                                String idempotencyKey) {
    var ctx = metrics.startSpan("CardAuthorizationService.authorize");

    // Step 0: Idempotency check
    return authTxnRepo.findByIdempotencyKey(idempotencyKey)
        .compose(existing -> {
          if (existing != null) {
            return Future.failedFuture(new ConflictException("Duplicate transaction: " + idempotencyKey));
          }

          // Step 1: Get credit account (Redis-first)
          return getCreditAccount(cardNumber);
        })
        .compose(account -> {
          // Step 2: Card status check
          if (!"ACTIVE".equals(account.getStatus())) {
            String code = DeclineCodeMapper.toDeclineCode(DeclineCodeMapper.DeclineReason.CARD_INACTIVE);
            return declineAndFail(cardNumber, merchantId, amount, currency, idempotencyKey,
                posEntryMode, mcc, code, "Card status: " + account.getStatus());
          }

          // Step 3: Available credit check
          if (account.getAvailableCredit() < amount) {
            return declineAndFail(cardNumber, merchantId, amount, currency, idempotencyKey,
                posEntryMode, mcc,
                DeclineCodeMapper.toDeclineCode(DeclineCodeMapper.DeclineReason.INSUFFICIENT_FUNDS),
                "Insufficient available credit");
          }

          // Step 4: Velocity check (Redis INCR + EXPIRE)
          return checkVelocity(cardNumber).compose(withinLimit -> {
            if (!withinLimit) {
              return declineAndFail(cardNumber, merchantId, amount, currency, idempotencyKey,
                  posEntryMode, mcc,
                  DeclineCodeMapper.toDeclineCode(DeclineCodeMapper.DeclineReason.VELOCITY_EXCEEDED),
                  "Velocity limit exceeded");
            }

            // Step 5: Atomic DB reserve + txn insert in transaction
            CardAuthTransaction pending = CardAuthTransaction.builder()
                .cardNumber(cardNumber)
                .merchantId(merchantId)
                .amount(amount)
                .currency(currency != null ? currency : "IDR")
                .posEntryMode(posEntryMode)
                .mcc(mcc)
                .idempotencyKey(idempotencyKey)
                .build();

            return insertAuthAndReserve(pending, account);
          });
        })
        .onSuccess(txn -> {
          // Step 6: Invalidate Redis cache
          redis.delete(String.format(REDIS_CREDIT_KEY, cardNumber));

          // Step 7: Publish Kafka event for async fraud scoring
          publishTxnCreated(txn);

          metrics.completeSpanSuccess(ctx, "authorize", "Approved: " + txn.getAuthCode());
        })
        .onFailure(e -> {
          if (!(e instanceof StatusRuntimeException)) {
            log.error("Authorization failed for card {}: {}", cardNumber, e.getMessage());
          }
          metrics.completeSpanError(ctx, "authorize", e.getMessage());
        });
  }

  @Override
  public Future<CardAuthTransaction> reverse(String txnId, String cardNumber, Long amount, String idempotencyKey) {
    var ctx = metrics.startSpan("CardAuthorizationService.reverse");

    return authTxnRepo.findById(txnId)
        .compose(txn -> {
          if (txn == null) {
            return Future.failedFuture(new BadRequestException("Transaction not found: " + txnId));
          }
          if (!"APPROVED".equals(txn.getStatus())) {
            return Future.failedFuture(new BadRequestException("Transaction cannot be reversed, status: " + txn.getStatus()));
          }
          return creditAccountRepo.releaseCredit(cardNumber, amount)
              .compose(released -> authTxnRepo.reverse(txnId))
              .compose(reversed -> {
                redis.delete(String.format(REDIS_CREDIT_KEY, cardNumber));
                return Future.succeededFuture(reversed);
              });
        })
        .onSuccess(v -> metrics.completeSpanSuccess(ctx, "reverse", "Reversed: " + txnId))
        .onFailure(e -> metrics.completeSpanError(ctx, "reverse", e.getMessage()));
  }

  private Future<CardCreditAccount> getCreditAccount(String cardNumber) {
    String redisKey = String.format(REDIS_CREDIT_KEY, cardNumber);

    return redis.get(redisKey)
        .compose(cached -> {
          if (cached != null) {
            try {
              CardCreditAccount account = CardCreditAccount.fromJson(new JsonObject(cached));
              if (account != null) {
                return Future.succeededFuture(account);
              }
            } catch (Exception e) {
              log.warn("Failed to deserialize cached credit account, falling back to DB");
            }
          }
          // Cache miss: fetch from DB
          return creditAccountRepo.findByCardNumber(cardNumber)
              .compose(account -> {
                if (account == null) {
                  return Future.failedFuture(new BadRequestException("Credit account not found for card: " + cardNumber));
                }
                return redis.set(String.format(REDIS_CREDIT_KEY, cardNumber),
                        account.toJson().encode(), REDIS_CACHE_TTL)
                    .map(v -> account);
              });
        });
  }

  private Future<Boolean> checkVelocity(String cardNumber) {
    String key = String.format(REDIS_VELOCITY_KEY, cardNumber);
    return redis.incr(key)
        .compose(count -> {
          if (count == 1) {
            return redis.expire(key, VELOCITY_WINDOW_TTL).map(v -> true);
          }
          return Future.succeededFuture(count <= VELOCITY_LIMIT);
        });
  }

  private Future<CardAuthTransaction> insertAuthAndReserve(CardAuthTransaction pending, CardCreditAccount account) {
    return authTxnRepo.insertPending(pending)
        .compose(inserted -> {
          // Reserve credit
          return creditAccountRepo.decrementAvailableCredit(pending.getCardNumber(), pending.getAmount())
              .compose(reserved -> {
                if (reserved == null) {
                  // Credit reserve failed — decline the pending txn
                  return authTxnRepo.decline(inserted.getTxnId().toString(),
                          DeclineCodeMapper.toDeclineCode(DeclineCodeMapper.DeclineReason.INSUFFICIENT_FUNDS))
                      .map(v -> inserted);
                }
                // Approve
                return authTxnRepo.approve(inserted.getTxnId().toString(), generateAuthCode())
                    .map(approved -> {
                      if (approved == null) return inserted;
                      return approved;
                    });
              });
        })
        .recover(err -> {
          log.error("Authorization atomic sequence failed: {}", err.getMessage());
          return Future.failedFuture(new FailedPreconditionException("Authorization processing failed: " + err.getMessage()));
        });
  }

  private Future<CardAuthTransaction> declineAndFail(String cardNumber, Integer merchantId, Long amount,
                                                       String currency, String idempotencyKey,
                                                       String posEntryMode, String mcc,
                                                       String declineCode, String reason) {
    // Skip DB insert for declined txns (clean failure)
    DeclineCodeMapper.DeclineReason reasonEnum = mapCodeToReason(declineCode);
    return Future.failedFuture(
        new FailedPreconditionException(reason != null ? reason : DeclineCodeMapper.toDescription(reasonEnum)));
  }

  private void publishTxnCreated(CardAuthTransaction txn) {
    try {
      JsonObject payload = new JsonObject()
          .put("txn_id", txn.getTxnId().toString())
          .put("card_number", txn.getCardNumber())
          .put("amount", txn.getAmount())
          .put("currency", txn.getCurrency())
          .put("mcc", txn.getMcc())
          .put("merchant_id", txn.getMerchantId())
          .put("status", txn.getStatus())
          .put("auth_code", txn.getAuthCode())
          .put("timestamp", txn.getTxnTime() != null ? txn.getTxnTime().toString() : null);

      kafkaService.sendMessage("card.txn.created", txn.getTxnId().toString(), payload);
    } catch (Exception e) {
      log.warn("Failed to publish txn.created event: {}", e.getMessage());
    }
  }

  private String generateAuthCode() {
    return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }

  private DeclineCodeMapper.DeclineReason mapCodeToReason(String code) {
    return switch (code) {
      case "51" -> DeclineCodeMapper.DeclineReason.INSUFFICIENT_FUNDS;
      case "54" -> DeclineCodeMapper.DeclineReason.CARD_EXPIRED;
      case "61" -> DeclineCodeMapper.DeclineReason.VELOCITY_EXCEEDED;
      case "62" -> DeclineCodeMapper.DeclineReason.CARD_INACTIVE;
      case "41" -> DeclineCodeMapper.DeclineReason.CARD_LOST;
      case "43" -> DeclineCodeMapper.DeclineReason.CARD_STOLEN;
      case "57" -> DeclineCodeMapper.DeclineReason.CANCELLED;
      case "94" -> DeclineCodeMapper.DeclineReason.DUPLICATE_TRANSACTION;
      case "59" -> DeclineCodeMapper.DeclineReason.SUSPICIOUS_FRAUD;
      case "05" -> DeclineCodeMapper.DeclineReason.DO_NOT_HONOR;
      default -> DeclineCodeMapper.DeclineReason.DO_NOT_HONOR;
    };
  }
}
