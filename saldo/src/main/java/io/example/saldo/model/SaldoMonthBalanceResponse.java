package io.example.saldo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SaldoMonthBalanceResponse {
  private String month;
  private Long totalBalance;

  public static SaldoMonthBalanceResponse from(SaldoStats.MonthBalance m) {
    if (m == null) return null;
    return SaldoMonthBalanceResponse.builder()
        .month(m.getMonth())
        .totalBalance(m.getTotalBalance())
        .build();
  }
}
