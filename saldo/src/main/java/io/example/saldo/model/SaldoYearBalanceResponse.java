package io.example.saldo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SaldoYearBalanceResponse {
  private String year;
  private Long totalBalance;

  public static SaldoYearBalanceResponse from(SaldoStats.YearBalance y) {
    if (y == null) return null;
    return SaldoYearBalanceResponse.builder()
        .year(y.getYear())
        .totalBalance(y.getTotalBalance())
        .build();
  }
}
