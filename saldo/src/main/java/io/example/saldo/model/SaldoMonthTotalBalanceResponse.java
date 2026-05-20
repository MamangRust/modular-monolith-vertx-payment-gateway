package io.example.saldo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SaldoMonthTotalBalanceResponse {
  private String month;
  private String year;
  private Long totalBalance;

  public static SaldoMonthTotalBalanceResponse from(SaldoStats.MonthTotalBalance m) {
    if (m == null) return null;
    return SaldoMonthTotalBalanceResponse.builder()
        .month(m.getMonth())
        .year(m.getYear())
        .totalBalance(m.getTotalBalance())
        .build();
  }
}
