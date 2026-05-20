package io.example.merchant.model;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MerchantDocument {
  private Integer id;
  private Integer merchantId;
  private String documentType;
  private String documentUrl;
  private String status;
  private String note;
  private Timestamp createdAt;
  private Timestamp updatedAt;
  private Timestamp deletedAt;

  public JsonObject toJson() {
    JsonObject json = new JsonObject()
        .put("id", id)
        .put("merchant_id", merchantId)
        .put("document_type", documentType)
        .put("document_url", documentUrl)
        .put("status", status)
        .put("note", note);

    if (createdAt != null) {
      json.put("created_at", createdAt.toString());
    }
    if (updatedAt != null) {
      json.put("updated_at", updatedAt.toString());
    }
    if (deletedAt != null) {
      json.put("deleted_at", deletedAt.toString());
    }

    return json;
  }

  public static MerchantDocument fromJson(JsonObject json) {
    if (json == null) {
      return null;
    }

    MerchantDocument doc = new MerchantDocument();
    doc.setId(json.getInteger("id"));
    doc.setMerchantId(json.getInteger("merchant_id"));
    doc.setDocumentType(json.getString("document_type"));
    doc.setDocumentUrl(json.getString("document_url"));
    doc.setStatus(json.getString("status"));
    doc.setNote(json.getString("note"));

    doc.setCreatedAt(parseTimestamp(json, "created_at"));
    doc.setUpdatedAt(parseTimestamp(json, "updated_at"));
    doc.setDeletedAt(parseTimestamp(json, "deleted_at"));

    return doc;
  }

  public static MerchantDocument fromRow(Row row) {
    if (row == null)
      return null;

    Integer id = row.getInteger("id");
    if (id == null) {
      id = row.getInteger("document_id");
    }
    Integer merchantId = row.getInteger("merchant_id");
    String documentType = row.getString("document_type");
    String documentUrl = row.getString("document_url");
    String status = row.getString("status");
    String note = row.getString("note");

    Timestamp createdAt = null;
    LocalDateTime createdAtLocal = row.get(LocalDateTime.class, "created_at");
    if (createdAtLocal != null) {
      createdAt = Timestamp.valueOf(createdAtLocal);
    }

    Timestamp updatedAt = null;
    LocalDateTime updatedAtLocal = row.get(LocalDateTime.class, "updated_at");
    if (updatedAtLocal != null) {
      updatedAt = Timestamp.valueOf(updatedAtLocal);
    }

    Timestamp deletedAt = null;
    LocalDateTime deletedAtLocal = row.get(LocalDateTime.class, "deleted_at");
    if (deletedAtLocal != null) {
      deletedAt = Timestamp.valueOf(deletedAtLocal);
    }

    return MerchantDocument.builder()
        .id(id)
        .merchantId(merchantId)
        .documentType(documentType)
        .documentUrl(documentUrl)
        .status(status)
        .note(note)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .deletedAt(deletedAt)
        .build();
  }

  private static Timestamp parseTimestamp(JsonObject json, String field) {
    Object value = json.getValue(field);

    if (value == null) {
      return null;
    }

    if (value instanceof Timestamp ts) {
      return ts;
    }

    if (value instanceof String str && !str.isBlank()) {
      try {
        return Timestamp.from(Instant.parse(str));
      } catch (DateTimeParseException e) {
        return null;
      }
    }

    if (value instanceof Number num) {
      return new Timestamp(num.longValue());
    }

    return null;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
