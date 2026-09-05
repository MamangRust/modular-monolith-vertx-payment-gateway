package io.example.transfer.model;

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
public class Transfer {
  private Integer id;
  private String transferNo;
  private String transferFrom;
  private String transferTo;
  private Long transferAmount;
  private String idempotencyKey;
  private String status;
  
  private OffsetDateTime transferTime;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
  private OffsetDateTime deletedAt;

  public JsonObject toJson() {
    JsonObject json = new JsonObject()
        .put("id", id)
        .put("transfer_no", transferNo)
        .put("transfer_from", transferFrom)
        .put("transfer_to", transferTo)
        .put("transfer_amount", transferAmount)
        .put("status", status);

    if (transferTime != null) json.put("transfer_time", transferTime.toString());
    if (createdAt != null) json.put("created_at", createdAt.toString());
    if (updatedAt != null) json.put("updated_at", updatedAt.toString());
    if (deletedAt != null) json.put("deleted_at", deletedAt.toString());

    return json;
  }

  public static Transfer fromJson(JsonObject json) {
    if (json == null) return null;

    return Transfer.builder()
        .id(json.getInteger("id"))
        .transferNo(json.getString("transfer_no"))
        .transferFrom(json.getString("transfer_from"))
        .transferTo(json.getString("transfer_to"))
        .transferAmount(json.getLong("transfer_amount"))
        .status(json.getString("status"))
        .transferTime(parseTime(json.getString("transfer_time")))
        .createdAt(parseTime(json.getString("created_at")))
        .updatedAt(parseTime(json.getString("updated_at")))
        .deletedAt(parseTime(json.getString("deleted_at")))
        .build();
  }

  public static Transfer fromRow(Row row) {
    if (row == null) return null;

    // Fallback between 'transfer_id' and 'id' just in case
    Integer rowId = row.getInteger("transfer_id");
    if (rowId == null) {
      try { rowId = row.getInteger("id"); } catch (Exception ignored) {}
    }

    return Transfer.builder()
        .id(rowId)
        .transferNo(row.getUUID("transfer_no") != null ? row.getUUID("transfer_no").toString() : row.getString("transfer_no"))
        .transferFrom(row.getString("transfer_from"))
        .transferTo(row.getString("transfer_to"))
        .transferAmount(row.getLong("transfer_amount"))
        .idempotencyKey(row.getString("idempotency_key"))
        .status(row.getString("status"))
        .transferTime(toOffsetDateTime(row, "transfer_time"))
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
