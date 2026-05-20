package io.example.merchant.service;

import java.util.List;
import io.example.merchant.model.MerchantStats;
import io.example.merchant.model.MerchantTransactions;
import io.example.merchant.repository.MerchantStatsRepository;
import io.example.common.domain.PagedResult;
import io.example.common.observability.TracingMetrics;
import io.vertx.core.Future;

public class MerchantStatsService {
  private final MerchantStatsRepository repository;
  private final TracingMetrics metrics;

  public MerchantStatsService(MerchantStatsRepository repository, TracingMetrics metrics) {
    this.repository = repository;
    this.metrics = metrics;
  }

  // --- TRANSACTION FEEDS ---

  public Future<PagedResult<MerchantTransactions>> getTransactions(int page, int pageSize, String search) {
    var ctx = metrics.startSpan("MerchantStatsService.getTransactions");
    return repository.getTransactions(page, pageSize, search)
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getTransactions", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getTransactions", e.getMessage()));
  }

  public Future<PagedResult<MerchantTransactions>> getTransactionsByMerchantId(int page, int pageSize, String search, int merchantId) {
    var ctx = metrics.startSpan("MerchantStatsService.getTransactionsByMerchantId");
    return repository.getTransactionsByMerchantId(page, pageSize, search, merchantId)
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getTransactionsByMerchantId", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getTransactionsByMerchantId", e.getMessage()));
  }

  public Future<PagedResult<MerchantTransactions>> getTransactionsByApiKey(int page, int pageSize, String search, String apiKey) {
    var ctx = metrics.startSpan("MerchantStatsService.getTransactionsByApiKey");
    return repository.getTransactionsByApiKey(page, pageSize, search, apiKey)
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getTransactionsByApiKey", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getTransactionsByApiKey", e.getMessage()));
  }

  // --- ANALYTICAL METRICS ---

  public Future<List<MerchantStats.MonthAmount>> getMonthlyAmounts(int year, Integer merchantId, String apiKey) {
    var ctx = metrics.startSpan("MerchantStatsService.getMonthlyAmounts");
    return repository.getMonthlyAmounts(year, merchantId, apiKey)
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyAmounts", e.getMessage()));
  }

  public Future<List<MerchantStats.YearAmount>> getYearlyAmounts(int year, Integer merchantId, String apiKey) {
    var ctx = metrics.startSpan("MerchantStatsService.getYearlyAmounts");
    return repository.getYearlyAmounts(year, merchantId, apiKey)
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyAmounts", e.getMessage()));
  }

  public Future<List<MerchantStats.MonthMethod>> getMonthlyMethodAmounts(int year, Integer merchantId, String apiKey) {
    var ctx = metrics.startSpan("MerchantStatsService.getMonthlyMethodAmounts");
    return repository.getMonthlyMethodAmounts(year, merchantId, apiKey)
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyMethodAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyMethodAmounts", e.getMessage()));
  }

  public Future<List<MerchantStats.YearMethod>> getYearlyMethodAmounts(int year, Integer merchantId, String apiKey) {
    var ctx = metrics.startSpan("MerchantStatsService.getYearlyMethodAmounts");
    return repository.getYearlyMethodAmounts(year, merchantId, apiKey)
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyMethodAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyMethodAmounts", e.getMessage()));
  }

  public Future<List<MerchantStats.MonthAmount>> getMonthlyTotalAmounts(int year, Integer merchantId, String apiKey) {
    var ctx = metrics.startSpan("MerchantStatsService.getMonthlyTotalAmounts");
    return repository.getMonthlyTotalAmounts(year, merchantId, apiKey)
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTotalAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTotalAmounts", e.getMessage()));
  }

  public Future<List<MerchantStats.YearAmount>> getYearlyTotalAmounts(int year, Integer merchantId, String apiKey) {
    var ctx = metrics.startSpan("MerchantStatsService.getYearlyTotalAmounts");
    return repository.getYearlyTotalAmounts(year, merchantId, apiKey)
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTotalAmounts", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTotalAmounts", e.getMessage()));
  }
}
