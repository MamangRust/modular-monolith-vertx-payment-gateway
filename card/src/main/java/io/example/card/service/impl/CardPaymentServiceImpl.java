package io.example.card.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.card.model.CardPayment;
import io.example.card.repository.CardPaymentRepository;
import io.example.card.repository.CardCreditAccountRepository;
import io.example.card.service.CardPaymentService;
import io.example.common.exception.grpc.ConflictException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class CardPaymentServiceImpl implements CardPaymentService {
  private static final Logger log = LoggerFactory.getLogger(CardPaymentServiceImpl.class);

  private static final String REDIS_CREDIT_KEY = "card:%s:credit";

  private final CardPaymentRepository paymentRepo;
  private final CardCreditAccountRepository creditAccountRepo;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private final KafkaService kafkaService;

  @Override
  public Future<CardPayment> postPayment(String referenceId, String cardNumber, Long amount,
                                          String paymentChannel, Integer statementId) {
    var ctx = metrics.startSpan("CardPaymentService.postPayment");

    // Idempotency check with FOR UPDATE semantics
    return paymentRepo.findByReferenceId(referenceId)
        .compose(existing -> {
          if (existing != null) {
            return Future.succeededFuture(existing);
          }

          // Create payment record
          CardPayment payment = CardPayment.builder()
              .referenceId(referenceId)
              .cardNumber(cardNumber)
              .amount(amount)
              .paymentChannel(paymentChannel)
              .statementId(statementId)
              .build();

          return paymentRepo.insertPayment(payment)
              .compose(inserted -> {
                // Release credit limit
                return creditAccountRepo.releaseCredit(cardNumber, amount)
                    .compose(released -> {
                      // Invalidate Redis cache
                      return redis.delete(String.format(REDIS_CREDIT_KEY, cardNumber))
                          .map(v -> inserted);
                    });
              })
              .compose(inserted -> {
                // Publish payment.posted event
                publishPaymentPosted(inserted);
                return Future.succeededFuture(inserted);
              });
        })
        .onSuccess(p -> metrics.completeSpanSuccess(ctx, "postPayment", "Posted: " + referenceId))
        .onFailure(e -> metrics.completeSpanError(ctx, "postPayment", e.getMessage()));
  }

  @Override
  public Future<List<CardPayment>> getPaymentHistory(String cardNumber, int page, int pageSize) {
    var ctx = metrics.startSpan("CardPaymentService.getPaymentHistory");
    int offset = (page - 1) * pageSize;
    return paymentRepo.findByCardNumber(cardNumber, pageSize, offset)
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getPaymentHistory", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getPaymentHistory", e.getMessage()));
  }

  @Override
  public Future<Integer> countPayments(String cardNumber) {
    return paymentRepo.countByCardNumber(cardNumber);
  }

  private void publishPaymentPosted(CardPayment payment) {
    try {
      JsonObject payload = new JsonObject()
          .put("payment_id", payment.getPaymentId() != null ? payment.getPaymentId().toString() : null)
          .put("reference_id", payment.getReferenceId())
          .put("card_number", payment.getCardNumber())
          .put("amount", payment.getAmount())
          .put("payment_channel", payment.getPaymentChannel())
          .put("status", payment.getStatus())
          .put("timestamp", payment.getPaymentTime() != null ? payment.getPaymentTime().toString() : null);

      kafkaService.sendMessage("card.payment.posted", payment.getReferenceId(), payload);
    } catch (Exception e) {
      log.warn("Failed to publish payment.posted event: {}", e.getMessage());
    }
  }
}
