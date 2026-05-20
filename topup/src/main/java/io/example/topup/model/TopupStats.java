package io.example.topup.model;

import io.vertx.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class TopupStats {

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class MonthAmount {
    private String month;
    private Long totalAmount;

    public static MonthAmount fromRow(Row row) {
      if (row == null) return null;
      
      Object amtVal = row.getValue("total_amount");
      Long amt = (amtVal instanceof Number num) ? num.longValue() : 0L;

      return MonthAmount.builder()
          .month(row.getString("month"))
          .totalAmount(amt)
          .build();
    }
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class YearAmount {
    private String year;
    private Long totalAmount;

    public static YearAmount fromRow(Row row) {
      if (row == null) return null;
      
      Object yearVal = row.getValue("year");
      String yearStr = yearVal != null ? yearVal.toString() : "";
      if (yearStr.contains(".")) yearStr = yearStr.split("\\.")[0];

      Object amtVal = row.getValue("total_amount");
      Long amt = (amtVal instanceof Number num) ? num.longValue() : 0L;

      return YearAmount.builder()
          .year(yearStr)
          .totalAmount(amt)
          .build();
    }
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class MonthMethod {
    private String month;
    private String topupMethod;
    private Long totalTopups;
    private Long totalAmount;

    public static MonthMethod fromRow(Row row) {
      if (row == null) return null;
      
      Object totalTopupsVal = row.getValue("total_topups");
      Long tops = (totalTopupsVal instanceof Number num) ? num.longValue() : 0L;
      
      Object totalAmtVal = row.getValue("total_amount");
      Long amt = (totalAmtVal instanceof Number num) ? num.longValue() : 0L;

      return MonthMethod.builder()
          .month(row.getString("month"))
          .topupMethod(row.getString("topup_method"))
          .totalTopups(tops)
          .totalAmount(amt)
          .build();
    }
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class YearMethod {
    private String year;
    private String topupMethod;
    private Long totalTopups;
    private Long totalAmount;

    public static YearMethod fromRow(Row row) {
      if (row == null) return null;
      
      Object yearVal = row.getValue("year");
      String yearStr = yearVal != null ? yearVal.toString() : "";
      if (yearStr.contains(".")) yearStr = yearStr.split("\\.")[0];

      Object totalTopupsVal = row.getValue("total_topups");
      Long tops = (totalTopupsVal instanceof Number num) ? num.longValue() : 0L;
      
      Object totalAmtVal = row.getValue("total_amount");
      Long amt = (totalAmtVal instanceof Number num) ? num.longValue() : 0L;

      return YearMethod.builder()
          .year(yearStr)
          .topupMethod(row.getString("topup_method"))
          .totalTopups(tops)
          .totalAmount(amt)
          .build();
    }
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class MonthStatus {
    private String year;
    private String month;
    private Long totalCount;
    private Long totalAmount;

    public static MonthStatus fromRow(Row row, String countCol) {
      if (row == null) return null;
      
      Object countVal = row.getValue(countCol);
      Long count = (countVal instanceof Number num) ? num.longValue() : 0L;
      
      Object amtVal = row.getValue("total_amount");
      Long amt = (amtVal instanceof Number num) ? num.longValue() : 0L;

      return MonthStatus.builder()
          .year(row.getString("year"))
          .month(row.getString("month"))
          .totalCount(count)
          .totalAmount(amt)
          .build();
    }
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class YearStatus {
    private String year;
    private Long totalCount;
    private Long totalAmount;

    public static YearStatus fromRow(Row row, String countCol) {
      if (row == null) return null;
      
      Object countVal = row.getValue(countCol);
      Long count = (countVal instanceof Number num) ? num.longValue() : 0L;
      
      Object amtVal = row.getValue("total_amount");
      Long amt = (amtVal instanceof Number num) ? num.longValue() : 0L;

      return YearStatus.builder()
          .year(row.getString("year"))
          .totalCount(count)
          .totalAmount(amt)
          .build();
    }
  }
}
