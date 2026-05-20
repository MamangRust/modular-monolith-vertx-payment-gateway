package io.example.topup.repository;

import java.util.List;
import io.example.topup.model.TopupStats;
import io.vertx.core.Future;
import pb.topup.Topup.FindYearTopupCardNumber;

public interface TopupStatsByCardAmountRepository {
  Future<List<TopupStats.MonthAmount>> getMonthlyTopupAmountsByCard(FindYearTopupCardNumber req);
  Future<List<TopupStats.YearAmount>> getYearlyTopupAmountsByCard(FindYearTopupCardNumber req);
}
