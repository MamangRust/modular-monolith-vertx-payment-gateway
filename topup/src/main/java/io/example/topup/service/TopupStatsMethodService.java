package io.example.topup.service;

import java.util.List;
import io.example.topup.model.TopupStats;
import io.vertx.core.Future;
import pb.topup.Topup.FindYearTopupCardNumber;
import pb.topup.Topup.FindYearTopupStatus;

public interface TopupStatsMethodService {
  Future<List<TopupStats.MonthMethod>> getMonthlyTopupMethods(FindYearTopupStatus req);
  Future<List<TopupStats.YearMethod>> getYearlyTopupMethods(FindYearTopupStatus req);
  Future<List<TopupStats.MonthMethod>> getMonthlyTopupMethodsByCard(FindYearTopupCardNumber req);
  Future<List<TopupStats.YearMethod>> getYearlyTopupMethodsByCard(FindYearTopupCardNumber req);
}
