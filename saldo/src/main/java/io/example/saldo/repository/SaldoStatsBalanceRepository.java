package io.example.saldo.repository;

import java.util.List;
import io.example.saldo.model.SaldoStats;
import io.vertx.core.Future;

public interface SaldoStatsBalanceRepository {
  Future<List<SaldoStats.MonthBalance>> getMonthlySaldoBalances(Integer year);
  Future<List<SaldoStats.YearBalance>> getYearlySaldoBalances(Integer endYear);
}
