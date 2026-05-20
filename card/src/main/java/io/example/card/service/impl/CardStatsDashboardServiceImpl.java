package io.example.card.service.impl;

import io.example.card.model.CardStats;
import io.example.card.repository.*;
import io.example.card.service.CardStatsDashboardService;
import io.example.common.domain.ApiResponse;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import java.time.Duration;

public class CardStatsDashboardServiceImpl implements CardStatsDashboardService {
  private final CardDashboardBalanceRepository balanceRepo;
  private final CardDashboardTopupRepository topupRepo;
  private final CardDashboardWithdrawRepository withdrawRepo;
  private final CardDashboardTransactionRepository transactionRepo;
  private final CardDashboardTransferRepository transferRepo;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final Duration CACHE_TTL = Duration.ofMinutes(15);

  public CardStatsDashboardServiceImpl(
      CardDashboardBalanceRepository balanceRepo,
      CardDashboardTopupRepository topupRepo,
      CardDashboardWithdrawRepository withdrawRepo,
      CardDashboardTransactionRepository transactionRepo,
      CardDashboardTransferRepository transferRepo,
      RedisService redis,
      TracingMetrics metrics) {
    this.balanceRepo = balanceRepo;
    this.topupRepo = topupRepo;
    this.withdrawRepo = withdrawRepo;
    this.transactionRepo = transactionRepo;
    this.transferRepo = transferRepo;
    this.redis = redis;
    this.metrics = metrics;
  }

  @Override
  public Future<ApiResponse<CardStats.Dashboard>> getDashboardCard() {
    var ctx = metrics.startSpan("CardStatsDashboardService.getDashboardCard");
    String cacheKey = "stats:dashboard:global";

    return redis.getJson(cacheKey, CardStats.Dashboard.class)
        .compose(cached -> {
          if (cached != null) return Future.succeededFuture(cached);
          
          Future<Long> f1 = balanceRepo.getTotalBalances();
          Future<Long> f2 = topupRepo.getTotalTopAmount();
          Future<Long> f3 = withdrawRepo.getTotalWithdrawAmount();
          Future<Long> f4 = transactionRepo.getTotalTransactionAmount();
          Future<Long> f5 = transferRepo.getTotalTransferAmount();

          return Future.all(f1, f2, f3, f4, f5)
              .map(cf -> new CardStats.Dashboard(
                  cf.resultAt(0),
                  cf.resultAt(1),
                  cf.resultAt(2),
                  cf.resultAt(3),
                  cf.resultAt(4)
              ))
              .compose(dash -> redis.setJson(cacheKey, dash, CACHE_TTL).map(v -> dash));
        })
        .map(dash -> ApiResponse.success("Dashboard data retrieved successfully", dash))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getDashboardCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getDashboardCard", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<CardStats.DashboardByCardNumber>> getDashboardCardByCardNumber(String cardNumber) {
    var ctx = metrics.startSpan("CardStatsDashboardService.getDashboardCardByCardNumber");
    String cacheKey = "stats:dashboard:card:" + cardNumber;

    return redis.getJson(cacheKey, CardStats.DashboardByCardNumber.class)
        .compose(cached -> {
          if (cached != null) return Future.succeededFuture(cached);

          Future<Long> f1 = balanceRepo.getTotalBalanceByCardNumber(cardNumber);
          Future<Long> f2 = topupRepo.getTotalTopupAmountByCardNumber(cardNumber);
          Future<Long> f3 = withdrawRepo.getTotalWithdrawAmountByCardNumber(cardNumber);
          Future<Long> f4 = transactionRepo.getTotalTransactionAmountByCardNumber(cardNumber);
          Future<Long> f5 = transferRepo.getTotalTransferAmountBySender(cardNumber);
          Future<Long> f6 = transferRepo.getTotalTransferAmountByReceiver(cardNumber);

          return Future.all(f1, f2, f3, f4, f5, f6)
              .map(cf -> new CardStats.DashboardByCardNumber(
                  cf.resultAt(0),
                  cf.resultAt(1),
                  cf.resultAt(2),
                  cf.resultAt(3),
                  cf.resultAt(4),
                  cf.resultAt(5)
              ))
              .compose(dash -> redis.setJson(cacheKey, dash, CACHE_TTL).map(v -> dash));
        })
        .map(dash -> ApiResponse.success("Dashboard data by card retrieved successfully", dash))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getDashboardCardByCardNumber", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getDashboardCardByCardNumber", e.getMessage()));
  }
}
