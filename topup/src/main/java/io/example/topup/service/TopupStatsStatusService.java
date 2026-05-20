package io.example.topup.service;

import java.util.List;
import io.example.topup.model.TopupStats;
import io.vertx.core.Future;
import pb.topup.Topup.FindMonthlyTopupStatus;
import pb.topup.Topup.FindMonthlyTopupStatusCardNumber;
import pb.topup.Topup.FindYearTopupStatus;
import pb.topup.Topup.FindYearTopupStatusCardNumber;

public interface TopupStatsStatusService {
  Future<List<TopupStats.MonthStatus>> getMonthlyTopupStatus(FindMonthlyTopupStatus req, String status);
  Future<List<TopupStats.YearStatus>> getYearlyTopupStatus(FindYearTopupStatus req, String status);
  Future<List<TopupStats.MonthStatus>> getMonthlyTopupStatusByCard(FindMonthlyTopupStatusCardNumber req, String status);
  Future<List<TopupStats.YearStatus>> getYearlyTopupStatusByCard(FindYearTopupStatusCardNumber req, String status);
}
