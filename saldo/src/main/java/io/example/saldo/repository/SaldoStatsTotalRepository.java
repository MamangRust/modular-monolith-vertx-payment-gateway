package io.example.saldo.repository;

import java.util.List;
import io.example.saldo.model.SaldoStats;
import io.vertx.core.Future;
import io.example.saldo.domain.requests.MonthTotalSaldoBalance;

public interface SaldoStatsTotalRepository {
  Future<List<SaldoStats.MonthTotalBalance>> getMonthlyTotalSaldoBalance(MonthTotalSaldoBalance req);
  Future<List<SaldoStats.YearTotalBalance>> getYearlyTotalSaldoBalances(Integer currentYear);
}
