package io.example.card.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.card.model.CardCreditAccount;
import io.example.card.repository.CardCreditAccountRepository;
import io.example.card.service.CreditLimitService;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@RequiredArgsConstructor
public class CreditLimitServiceImpl implements CreditLimitService {
  private static final Logger log = LoggerFactory.getLogger(CreditLimitServiceImpl.class);

  private static final String REDIS_CREDIT_KEY = "card:%s:credit";
  private static final Duration REDIS_CACHE_TTL = Duration.ofSeconds(60);

  private final CardCreditAccountRepository creditAccountRepo;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private final KafkaService kafkaService;

  @Override
  public Future<CardCreditAccount> getLimit(String cardNumber) {
    var ctx = metrics.startSpan("CreditLimitService.getLimit");
    String redisKey = String.format(REDIS_CREDIT_KEY, cardNumber);

    return redis.get(redisKey)
        .compose(cached -> {
          if (cached != null) {
            try {
              CardCreditAccount account = CardCreditAccount.fromJson(new JsonObject(cached));
              if (account != null) return Future.succeededFuture(account);
            } catch (Exception e) {
              log.warn("Cache deserialize failed for {}", redisKey);
            }
          }
          return creditAccountRepo.findByCardNumber(cardNumber)
              .compose(account -> {
                if (account == null) {
                  return Future.failedFuture(new NotFoundException("Credit account not found: " + cardNumber));
                }
                return redis.set(redisKey, account.toJson().encode(), REDIS_CACHE_TTL)
                    .map(v -> account);
              });
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getLimit", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getLimit", e.getMessage()));
  }

  @Override
  public Future<CardCreditAccount> setLimit(String cardNumber, Long creditLimit,
                                             Integer billingCycleDay, Integer annualRateBps) {
    var ctx = metrics.startSpan("CreditLimitService.setLimit");

    return creditAccountRepo.findByCardNumber(cardNumber)
        .compose(existing -> {
          if (existing != null) {
            // Update existing account
            return creditAccountRepo.setCreditLimit(cardNumber, creditLimit);
          }
          // Create new account
          return creditAccountRepo.createAccount(cardNumber, creditLimit,
              billingCycleDay != null ? billingCycleDay : 1,
              annualRateBps != null ? annualRateBps : 1800);
        })
        .compose(account -> {
          redis.delete(String.format(REDIS_CREDIT_KEY, cardNumber));
          publishLimitChanged(cardNumber, account.getCreditLimit());
          return Future.succeededFuture(account);
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "setLimit", "Success"))
        .onFailure(e -> {
          log.error("setLimit failed for card {}", cardNumber, e);
          metrics.completeSpanError(ctx, "setLimit", e.getMessage());
        });
  }

  @Override
  public Future<CardCreditAccount> adjustLimit(String cardNumber, Long delta) {
    var ctx = metrics.startSpan("CreditLimitService.adjustLimit");

    return creditAccountRepo.adjustCreditLimit(cardNumber, delta)
        .compose(account -> {
          if (account == null) {
            return Future.failedFuture(new NotFoundException("Credit account not found: " + cardNumber));
          }
          redis.delete(String.format(REDIS_CREDIT_KEY, cardNumber));
          publishLimitChanged(cardNumber, account.getCreditLimit());
          return Future.succeededFuture(account);
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "adjustLimit", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "adjustLimit", e.getMessage()));
  }

  private void publishLimitChanged(String cardNumber, Long newLimit) {
    try {
      JsonObject payload = new JsonObject()
          .put("card_number", cardNumber)
          .put("credit_limit", newLimit)
          .put("timestamp", java.time.Instant.now().toString());

      kafkaService.sendMessage("card.limit.changed", cardNumber, payload);
    } catch (Exception e) {
      log.warn("Failed to publish limit.changed event: {}", e.getMessage());
    }
  }
}
