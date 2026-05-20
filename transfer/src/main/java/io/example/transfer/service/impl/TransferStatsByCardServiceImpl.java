package io.example.transfer.service.impl;

import java.time.Duration;
import java.util.List;

import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.transfer.model.TransferStats;
import io.example.transfer.repository.TransferStatsByCardAmountReceiverRepository;
import io.example.transfer.repository.TransferStatsByCardAmountSenderRepository;
import io.example.transfer.repository.TransferStatsByCardStatusRepository;
import io.example.transfer.service.TransferStatsByCardAmountService;
import io.example.transfer.service.TransferStatsByCardStatusService;
import io.vertx.core.Future;

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

  public TransferStatsByCardServiceImpl(
      TransferStatsByCardAmountSenderRepository senderRepo,
      TransferStatsByCardAmountReceiverRepository receiverRepo,
      TransferStatsByCardStatusRepository statusRepo,
      RedisService redis,
      TracingMetrics metrics) {
    this.senderRepo = senderRepo;
    this.receiverRepo = receiverRepo;
    this.statusRepo = statusRepo;
    this.redis = redis;
    this.metrics = metrics;
  }

  @Override
  public Future<List<TransferStats.MonthAmount>> getMonthlySenderAmountsByCard(String card, int year) {
    String cacheKey = CACHE_PREFIX + "sender:monthly:" + card + ":" + year;
    var ctx = metrics.startSpan("TransferStatsByCardService.getMonthlySenderAmountsByCard");

    return redis.getJsonList(cacheKey, TransferStats.MonthAmount.class)
        .compose(cached -> {
          if (!cached.isEmpty()) {
            metrics.completeSpanSuccess(ctx, "getMonthlySenderAmountsByCard", "Success (from cache)");
            return Future.succeededFuture(cached);
          }
          return senderRepo.getMonthlySenderAmountsByCard(card, year)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlySenderAmountsByCard", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlySenderAmountsByCard", e.getMessage()));
        });
  }

  @Override
  public Future<List<TransferStats.MonthAmount>> getMonthlyReceiverAmountsByCard(String card, int year) {
    String cacheKey = CACHE_PREFIX + "receiver:monthly:" + card + ":" + year;
    var ctx = metrics.startSpan("TransferStatsByCardService.getMonthlyReceiverAmountsByCard");

    return redis.getJsonList(cacheKey, TransferStats.MonthAmount.class)
        .compose(cached -> {
          if (!cached.isEmpty()) {
            metrics.completeSpanSuccess(ctx, "getMonthlyReceiverAmountsByCard", "Success (from cache)");
            return Future.succeededFuture(cached);
          }
          return receiverRepo.getMonthlyReceiverAmountsByCard(card, year)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyReceiverAmountsByCard", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyReceiverAmountsByCard", e.getMessage()));
        });
  }

  @Override
  public Future<List<TransferStats.YearAmount>> getYearlySenderAmountsByCard(String card, int year) {
    String cacheKey = CACHE_PREFIX + "sender:yearly:" + card + ":" + year;
    var ctx = metrics.startSpan("TransferStatsByCardService.getYearlySenderAmountsByCard");

    return redis.getJsonList(cacheKey, TransferStats.YearAmount.class)
        .compose(cached -> {
          if (!cached.isEmpty()) {
            metrics.completeSpanSuccess(ctx, "getYearlySenderAmountsByCard", "Success (from cache)");
            return Future.succeededFuture(cached);
          }
          return senderRepo.getYearlySenderAmountsByCard(card, year)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlySenderAmountsByCard", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getYearlySenderAmountsByCard", e.getMessage()));
        });
  }

  @Override
  public Future<List<TransferStats.YearAmount>> getYearlyReceiverAmountsByCard(String card, int year) {
    String cacheKey = CACHE_PREFIX + "receiver:yearly:" + card + ":" + year;
    var ctx = metrics.startSpan("TransferStatsByCardService.getYearlyReceiverAmountsByCard");

    return redis.getJsonList(cacheKey, TransferStats.YearAmount.class)
        .compose(cached -> {
          if (!cached.isEmpty()) {
            metrics.completeSpanSuccess(ctx, "getYearlyReceiverAmountsByCard", "Success (from cache)");
            return Future.succeededFuture(cached);
          }
          return receiverRepo.getYearlyReceiverAmountsByCard(card, year)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyReceiverAmountsByCard", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyReceiverAmountsByCard", e.getMessage()));
        });
  }

  @Override
  public Future<List<TransferStats.MonthStatus>> getMonthlyStatusByCard(
      pb.transfer.Transfer.FindMonthlyTransferStatusCardNumber req, String status) {
    String cacheKey = CACHE_PREFIX + "status:monthly:" + req.getCardNumber() + ":" + req.getYear() + ":" + req.getMonth() + ":" + status;
    var ctx = metrics.startSpan("TransferStatsByCardService.getMonthlyStatusByCard");

    return redis.getJsonList(cacheKey, TransferStats.MonthStatus.class)
        .compose(cached -> {
          if (!cached.isEmpty()) {
            metrics.completeSpanSuccess(ctx, "getMonthlyStatusByCard", "Success (from cache)");
            return Future.succeededFuture(cached);
          }
          return statusRepo.getMonthlyStatusByCard(req, status)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyStatusByCard", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyStatusByCard", e.getMessage()));
        });
  }

  @Override
  public Future<List<TransferStats.YearStatus>> getYearlyStatusByCard(
      pb.transfer.Transfer.FindYearTransferStatusCardNumber req, String status) {
    String cacheKey = CACHE_PREFIX + "status:yearly:" + req.getCardNumber() + ":" + req.getYear() + ":" + status;
    var ctx = metrics.startSpan("TransferStatsByCardService.getYearlyStatusByCard");

    return redis.getJsonList(cacheKey, TransferStats.YearStatus.class)
        .compose(cached -> {
          if (!cached.isEmpty()) {
            metrics.completeSpanSuccess(ctx, "getYearlyStatusByCard", "Success (from cache)");
            return Future.succeededFuture(cached);
          }
          return statusRepo.getYearlyStatusByCard(req, status)
              .compose(res -> redis.setJsonList(cacheKey, res, CACHE_TTL).map(v -> res))
              .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyStatusByCard", "Success"))
              .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyStatusByCard", e.getMessage()));
        });
  }
}
