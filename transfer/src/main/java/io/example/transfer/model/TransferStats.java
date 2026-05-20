package io.example.transfer.model;

import io.vertx.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class TransferStats {

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MonthAmount {
    private String month;
    private Long totalAmount;

    public static MonthAmount fromRow(Row r) {
      Object val = r.getValue("total_transfer_amount");
      if (val == null) val = r.getValue("total_amount");
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
      Object yrVal = r.getValue("year");
      Object amtVal = r.getValue("total_transfer_amount");
      if (amtVal == null) amtVal = r.getValue("total_amount");
      return new YearAmount(
          yrVal != null ? yrVal.toString() : "",
          amtVal instanceof Number num ? num.longValue() : 0L
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
      Object cntVal = r.getValue(countCol);
      Object amtVal = r.getValue("total_amount");
      return new MonthStatus(
          r.getString("year"),
          r.getString("month"),
          cntVal instanceof Number n1 ? n1.longValue() : 0L,
          amtVal instanceof Number n2 ? n2.longValue() : 0L
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
      Object cntVal = r.getValue(countCol);
      Object amtVal = r.getValue("total_amount");
      return new YearStatus(
          r.getString("year"),
          cntVal instanceof Number n1 ? n1.longValue() : 0L,
          amtVal instanceof Number n2 ? n2.longValue() : 0L
      );
    }
  }
}
