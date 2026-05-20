package io.example.saldo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SaldoYearTotalBalanceResponse {
  private String year;
  private Long totalBalance;

  public static SaldoYearTotalBalanceResponse from(SaldoStats.YearTotalBalance y) {
    if (y == null) return null;
    return SaldoYearTotalBalanceResponse.builder()
        .year(y.getYear())
        .totalBalance(y.getTotalBalance())
        .build();
  }
}
