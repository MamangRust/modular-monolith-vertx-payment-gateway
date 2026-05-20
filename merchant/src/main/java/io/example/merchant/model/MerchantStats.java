package io.example.merchant.model;

import io.vertx.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class MerchantStats {

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class MonthAmount {
    private String month;
    private Long amount;

    public static MonthAmount fromRow(Row row) {
      return MonthAmount.builder()
          .month(String.valueOf(row.getValue("month")))
          .amount(row.getLong("amount"))
          .build();
    }

    public io.vertx.core.json.JsonObject toJson() {
      return io.vertx.core.json.JsonObject.mapFrom(this);
    }

    public static MonthAmount fromJson(io.vertx.core.json.JsonObject json) {
      return json.mapTo(MonthAmount.class);
    }
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class YearAmount {
    private String year;
    private Long amount;

    public static YearAmount fromRow(Row row) {
      return YearAmount.builder()
          .year(String.valueOf(row.getValue("year")))
          .amount(row.getLong("amount"))
          .build();
    }

    public io.vertx.core.json.JsonObject toJson() {
      return io.vertx.core.json.JsonObject.mapFrom(this);
    }

    public static YearAmount fromJson(io.vertx.core.json.JsonObject json) {
      return json.mapTo(YearAmount.class);
    }
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class MonthMethod {
    private String month;
    private String paymentMethod;
    private Long amount;

    public static MonthMethod fromRow(Row row) {
      return MonthMethod.builder()
          .month(String.valueOf(row.getValue("month")))
          .paymentMethod(row.getString("payment_method"))
          .amount(row.getLong("amount"))
          .build();
    }

    public io.vertx.core.json.JsonObject toJson() {
      return io.vertx.core.json.JsonObject.mapFrom(this);
    }

    public static MonthMethod fromJson(io.vertx.core.json.JsonObject json) {
      return json.mapTo(MonthMethod.class);
    }
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class YearMethod {
    private String year;
    private String paymentMethod;
    private Long amount;

    public static YearMethod fromRow(Row row) {
      return YearMethod.builder()
          .year(String.valueOf(row.getValue("year")))
          .paymentMethod(row.getString("payment_method"))
          .amount(row.getLong("amount"))
          .build();
    }

    public io.vertx.core.json.JsonObject toJson() {
      return io.vertx.core.json.JsonObject.mapFrom(this);
    }

    public static YearMethod fromJson(io.vertx.core.json.JsonObject json) {
      return json.mapTo(YearMethod.class);
    }
  }
}
