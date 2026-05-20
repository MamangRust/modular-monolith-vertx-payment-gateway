package io.example.saldo.service;

import java.util.List;
import io.example.saldo.model.SaldoStats;
import io.vertx.core.Future;

public interface SaldoStatsBalanceService {
  Future<List<SaldoStats.MonthBalance>> getMonthlySaldoBalances(Integer year);
  Future<List<SaldoStats.YearBalance>> getYearlySaldoBalances(Integer year);
}
