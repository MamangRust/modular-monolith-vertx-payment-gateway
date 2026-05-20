package io.example.saldo.service;

import java.util.List;
import io.example.saldo.model.SaldoStats;
import io.vertx.core.Future;
import io.example.saldo.domain.requests.MonthTotalSaldoBalance;

public interface SaldoStatsTotalService {
  Future<List<SaldoStats.MonthTotalBalance>> getMonthlyTotalSaldoBalance(MonthTotalSaldoBalance req);
  Future<List<SaldoStats.YearTotalBalance>> getYearlyTotalSaldoBalances(Integer year);
}
