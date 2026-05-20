package io.example.card.model;

import io.vertx.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class CardStats {

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class MonthAmount {
    private Integer month;
    private Long amount;

    public static MonthAmount fromRow(Row row) {
      return MonthAmount.builder()
          .month(row.getInteger("month"))
          .amount(row.getLong("amount"))
          .build();
    }
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class MonthBalance {
    private Integer month;
    private Long balance;

    public static MonthBalance fromRow(Row row) {
      return MonthBalance.builder()
          .month(row.getInteger("month"))
          .balance(row.getLong("balance"))
          .build();
    }
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class YearAmount {
    private Integer year;
    private Long amount;

    public static YearAmount fromRow(Row row) {
      return YearAmount.builder()
          .year(row.getInteger("year"))
          .amount(row.getLong("amount"))
          .build();
    }
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class YearlyBalance {
    private Integer year;
    private Long balance;

    public static YearlyBalance fromRow(Row row) {
      return YearlyBalance.builder()
          .year(row.getInteger("year"))
          .balance(row.getLong("balance"))
          .build();
    }
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Dashboard {
    private Long totalBalance;
    private Long totalTopup;
    private Long totalWithdraw;
    private Long totalTransaction;
    private Long totalTransfer;
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class DashboardByCardNumber {
    private Long totalBalance;
    private Long totalTopup;
    private Long totalWithdraw;
    private Long totalTransaction;
    private Long totalTransferSend;
    private Long totalTransferReceiver;
  }
}
