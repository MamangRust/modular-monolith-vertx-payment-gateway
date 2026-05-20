package io.example.withdraw.model;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Withdraw {
  private Integer id;
  private String withdrawNo;
  private String cardNumber;
  private Long withdrawAmount;
  private String status;

  private OffsetDateTime withdrawTime;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
  private OffsetDateTime deletedAt;

  public JsonObject toJson() {
    JsonObject json = new JsonObject()
        .put("id", id)
        .put("withdraw_no", withdrawNo)
        .put("card_number", cardNumber)
        .put("withdraw_amount", withdrawAmount)
        .put("status", status);

    if (withdrawTime != null) json.put("withdraw_time", withdrawTime.toString());
    if (createdAt != null) json.put("created_at", createdAt.toString());
    if (updatedAt != null) json.put("updated_at", updatedAt.toString());
    if (deletedAt != null) json.put("deleted_at", deletedAt.toString());

    return json;
  }

  public static Withdraw fromJson(JsonObject json) {
    if (json == null) return null;

    return Withdraw.builder()
        .id(json.getInteger("id"))
        .withdrawNo(json.getString("withdraw_no"))
        .cardNumber(json.getString("card_number"))
        .withdrawAmount(json.getLong("withdraw_amount"))
        .status(json.getString("status"))
        .withdrawTime(parseTime(json.getString("withdraw_time")))
        .createdAt(parseTime(json.getString("created_at")))
        .updatedAt(parseTime(json.getString("updated_at")))
        .deletedAt(parseTime(json.getString("deleted_at")))
        .build();
  }

  public static Withdraw fromRow(Row row) {
    if (row == null) return null;

    Integer rowId = row.getInteger("withdraw_id");
    if (rowId == null) {
      try { rowId = row.getInteger("id"); } catch (Exception ignored) {}
    }

    return Withdraw.builder()
        .id(rowId)
        .withdrawNo(row.getUUID("withdraw_no") != null ? row.getUUID("withdraw_no").toString() : row.getString("withdraw_no"))
        .cardNumber(row.getString("card_number"))
        .withdrawAmount(row.getLong("withdraw_amount"))
        .status(row.getString("status"))
        .withdrawTime(toOffsetDateTime(row, "withdraw_time"))
        .createdAt(toOffsetDateTime(row, "created_at"))
        .updatedAt(toOffsetDateTime(row, "updated_at"))
        .deletedAt(toOffsetDateTime(row, "deleted_at"))
        .build();
  }

  private static OffsetDateTime toOffsetDateTime(Row row, String column) {
    try {
      LocalDateTime ldt = row.getLocalDateTime(column);
      if (ldt != null) return ldt.atOffset(ZoneOffset.UTC);
    } catch (Exception e) {
      try {
        return row.getOffsetDateTime(column);
      } catch (Exception ignored) {}
    }
    return null;
  }

  private static OffsetDateTime parseTime(String str) {
    if (str == null || str.isBlank()) return null;
    try {
      return OffsetDateTime.parse(str);
    } catch (Exception e) {
      return null;
    }
  }
}
