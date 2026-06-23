package io.example.transfer.service.impl;

import java.time.Duration;
import java.util.List;

import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.transfer.domain.requests.MonthStatusTransferCardNumber;
import io.example.transfer.domain.requests.MonthYearCardNumber;
import io.example.transfer.domain.requests.YearStatusTransferCardNumber;
import io.example.transfer.model.TransferStats;
import io.example.transfer.repository.TransferStatsByCardAmountReceiverRepository;
import io.example.transfer.repository.TransferStatsByCardAmountSenderRepository;
import io.example.transfer.repository.TransferStatsByCardStatusRepository;
import io.example.transfer.service.TransferStatsByCardAmountService;
import io.example.transfer.service.TransferStatsByCardStatusService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TransferStatsByCardServiceImpl implements
    TransferStatsByCardAmountService,
    TransferStatsByCardStatusService {

  private final TransferStatsByCardAmountSenderRepository senderRepo;
  private final TransferStatsByCardAmountReceiverRepository receiverRepo;
  private final TransferStatsByCardStatusRepository statusRepo;
  private final RedisService redis;
  private final TracingMetrics metrics;

  private static final Duration CACHE_TTL = Duration.ofMinutes(10);
  private static final String CACHE_PREFIX = "transfer:stats:card:";

  @Override
  public Future<List<TransferStats.MonthAmount>> getMonthlySenderAmountsByCard(MonthYearCardNumber req) {
    String cacheKey = CACHE_PREFIX + "sender:monthly:" + req.getCardNumber() + ":" + req.getYear();
    var ctx = metrics.startSpan("TransferStatsByCardService.getMonthlySenderAmountsByCard");

    return redis.getJsonList(cacheKey, TransferStats.MonthAmount.class)
        .compose(cached -> {
          if (!cached.isEmpty())
            return Future.succeededFuture(cached);
          return senderRepo.getMonthlySenderAmountsByCard(req)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlySenderAmountsByCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlySenderAmountsByCard", e.getMessage()));
  }

  @Override
  public Future<List<TransferStats.MonthAmount>> getMonthlyReceiverAmountsByCard(MonthYearCardNumber req) {
    String cacheKey = CACHE_PREFIX + "receiver:monthly:" + req.getCardNumber() + ":" + req.getYear();
    var ctx = metrics.startSpan("TransferStatsByCardService.getMonthlyReceiverAmountsByCard");

    return redis.getJsonList(cacheKey, TransferStats.MonthAmount.class)
        .compose(cached -> {
          if (!cached.isEmpty())
            return Future.succeededFuture(cached);
          return receiverRepo.getMonthlyReceiverAmountsByCard(req)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyReceiverAmountsByCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyReceiverAmountsByCard", e.getMessage()));
  }

  @Override
  public Future<List<TransferStats.YearAmount>> getYearlySenderAmountsByCard(MonthYearCardNumber req) {
    String cacheKey = CACHE_PREFIX + "sender:yearly:" + req.getCardNumber() + ":" + req.getYear();
    var ctx = metrics.startSpan("TransferStatsByCardService.getYearlySenderAmountsByCard");

    return redis.getJsonList(cacheKey, TransferStats.YearAmount.class)
        .compose(cached -> {
          if (!cached.isEmpty())
            return Future.succeededFuture(cached);
          return senderRepo.getYearlySenderAmountsByCard(req)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlySenderAmountsByCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlySenderAmountsByCard", e.getMessage()));
  }

  @Override
  public Future<List<TransferStats.YearAmount>> getYearlyReceiverAmountsByCard(MonthYearCardNumber req) {
    String cacheKey = CACHE_PREFIX + "receiver:yearly:" + req.getCardNumber() + ":" + req.getYear();
    var ctx = metrics.startSpan("TransferStatsByCardService.getYearlyReceiverAmountsByCard");

    return redis.getJsonList(cacheKey, TransferStats.YearAmount.class)
        .compose(cached -> {
          if (!cached.isEmpty())
            return Future.succeededFuture(cached);
          return receiverRepo.getYearlyReceiverAmountsByCard(req)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyReceiverAmountsByCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyReceiverAmountsByCard", e.getMessage()));
  }

  @Override
  public Future<List<TransferStats.MonthStatus>> getMonthlyStatusByCard(MonthStatusTransferCardNumber req) {
    String cacheKey = CACHE_PREFIX + "status:monthly:" + req.getCardNumber() + ":" + req.getYear() + ":"
        + req.getMonth() + ":" + req.getStatus();
    var ctx = metrics.startSpan("TransferStatsByCardService.getMonthlyStatusByCard");

    return redis.getJsonList(cacheKey, TransferStats.MonthStatus.class)
        .compose(cached -> {
          if (!cached.isEmpty())
            return Future.succeededFuture(cached);
          return statusRepo.getMonthlyStatusByCard(req)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyStatusByCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyStatusByCard", e.getMessage()));
  }

  @Override
  public Future<List<TransferStats.YearStatus>> getYearlyStatusByCard(YearStatusTransferCardNumber req) {
    String cacheKey = CACHE_PREFIX + "status:yearly:" + req.getCardNumber() + ":" + req.getYear() + ":"
        + req.getStatus();
    var ctx = metrics.startSpan("TransferStatsByCardService.getYearlyStatusByCard");

    return redis.getJsonList(cacheKey, TransferStats.YearStatus.class)
        .compose(cached -> {
          if (!cached.isEmpty())
            return Future.succeededFuture(cached);
          return statusRepo.getYearlyStatusByCard(req)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyStatusByCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyStatusByCard", e.getMessage()));
  }
}