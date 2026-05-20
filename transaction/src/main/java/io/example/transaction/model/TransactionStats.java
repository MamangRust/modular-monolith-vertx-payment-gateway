package io.example.transaction.model;

import io.vertx.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class TransactionStats {

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MonthAmount {
    private String month;
    private Long totalAmount;

    public static MonthAmount fromRow(Row r) {
      Object val = r.getValue("total_amount");
      return new MonthAmount(
          r.getString("month"),
          val instanceof Number num ? num.longValue() : 0L
      );
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class YearAmount {
    private String year;
    private Long totalAmount;

    public static YearAmount fromRow(Row r) {
      Object yr = r.getValue("year");
      Object val = r.getValue("total_amount");
      return new YearAmount(
          yr != null ? yr.toString() : "",
          val instanceof Number num ? num.longValue() : 0L
      );
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MonthStatus {
    private String year;
    private String month;
    private Long totalCount;
    private Long totalAmount;

    public static MonthStatus fromRow(Row r, String countCol) {
      Object cnt = r.getValue(countCol);
      Object amt = r.getValue("total_amount");
      return new MonthStatus(
          r.getString("year"),
          r.getString("month"),
          cnt instanceof Number n1 ? n1.longValue() : 0L,
          amt instanceof Number n2 ? n2.longValue() : 0L
      );
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class YearStatus {
    private String year;
    private Long totalCount;
    private Long totalAmount;

    public static YearStatus fromRow(Row r, String countCol) {
      Object cnt = r.getValue(countCol);
      Object amt = r.getValue("total_amount");
      return new YearStatus(
          r.getString("year"),
          cnt instanceof Number n1 ? n1.longValue() : 0L,
          amt instanceof Number n2 ? n2.longValue() : 0L
      );
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MonthMethod {
    private String month;
    private String paymentMethod;
    private Long totalTransactions;
    private Long totalAmount;

    public static MonthMethod fromRow(Row r) {
      Object cnt = r.getValue("total_transactions");
      Object amt = r.getValue("total_amount");
      return new MonthMethod(
          r.getString("month"),
          r.getString("payment_method"),
          cnt instanceof Number n1 ? n1.longValue() : 0L,
          amt instanceof Number n2 ? n2.longValue() : 0L
      );
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class YearMethod {
    private String year;
    private String paymentMethod;
    private Long totalTransactions;
    private Long totalAmount;

    public static YearMethod fromRow(Row r) {
      Object yr = r.getValue("year");
      Object cnt = r.getValue("total_transactions");
      Object amt = r.getValue("total_amount");
      return new YearMethod(
          yr != null ? yr.toString() : "",
          r.getString("payment_method"),
          cnt instanceof Number n1 ? n1.longValue() : 0L,
          amt instanceof Number n2 ? n2.longValue() : 0L
      );
    }
  }
}
