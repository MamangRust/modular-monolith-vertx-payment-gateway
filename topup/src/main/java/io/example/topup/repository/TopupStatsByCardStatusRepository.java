package io.example.topup.repository;

import java.util.List;
import io.example.topup.model.TopupStats;
import io.vertx.core.Future;
import pb.topup.Topup.FindMonthlyTopupStatusCardNumber;
import pb.topup.Topup.FindYearTopupStatusCardNumber;

public interface TopupStatsByCardStatusRepository {
  Future<List<TopupStats.MonthStatus>> getMonthlyTopupStatusByCard(FindMonthlyTopupStatusCardNumber req, String status);
  Future<List<TopupStats.YearStatus>> getYearlyTopupStatusByCard(FindYearTopupStatusCardNumber req, String status);
}
