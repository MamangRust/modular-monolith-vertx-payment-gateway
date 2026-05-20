package io.example.saldo.model;

import io.vertx.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class SaldoStats {

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class MonthBalance {
    private String month;
    private Long totalBalance;

    public static MonthBalance fromRow(Row row) {
      if (row == null) return null;
      return MonthBalance.builder()
          .month(row.getString("month"))
          .totalBalance(row.getLong("total_balance"))
          .build();
    }
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class MonthTotalBalance {
    private String month;
    private String year;
    private Long totalBalance;

    public static MonthTotalBalance fromRow(Row row) {
      if (row == null) return null;
      return MonthTotalBalance.builder()
          .month(row.getString("month"))
          .year(row.getString("year"))
          .totalBalance(row.getLong("total_balance"))
          .build();
    }
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class YearBalance {
    private String year;
    private Long totalBalance;

    public static YearBalance fromRow(Row row) {
      if (row == null) return null;
      // Support both String or numerical outputs for year field based on grouping
      Object yr = row.getValue("year");
      return YearBalance.builder()
          .year(yr != null ? yr.toString() : "")
          .totalBalance(row.getLong("total_balance"))
          .build();
    }
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class YearTotalBalance {
    private String year;
    private Long totalBalance;

    public static YearTotalBalance fromRow(Row row) {
      if (row == null) return null;
      Object yr = row.getValue("year");
      return YearTotalBalance.builder()
          .year(yr != null ? yr.toString() : "")
          .totalBalance(row.getLong("total_balance"))
          .build();
    }
  }
}
