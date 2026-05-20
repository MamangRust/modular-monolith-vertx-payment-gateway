package io.example.topup.repository;

import java.util.List;
import io.example.topup.model.TopupStats;
import io.vertx.core.Future;
import pb.topup.Topup.FindYearTopupCardNumber;

public interface TopupStatsByCardMethodRepository {
  Future<List<TopupStats.MonthMethod>> getMonthlyTopupMethodsByCard(FindYearTopupCardNumber req);
  Future<List<TopupStats.YearMethod>> getYearlyTopupMethodsByCard(FindYearTopupCardNumber req);
}
