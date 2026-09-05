package io.example.card.model;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;

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
public class Card {
  private Integer id;
  private Integer userId;
  private String cardNumber;
  private String cardType;
  private String expireDate;
  private String cvv;
  private String cardProvider;
  private Timestamp createdAt;
  private Timestamp updatedAt;
  private Timestamp deletedAt;

  public JsonObject toJson() {
    JsonObject json = new JsonObject()
        .put("id", id)
        .put("user_id", userId)
        .put("card_number", cardNumber)
        .put("card_type", cardType)
        .put("expire_date", expireDate)
        .put("cvv", cvv)
        .put("card_provider", cardProvider);

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

  public static Card fromJson(JsonObject json) {
    if (json == null) {
      return null;
    }

    Card card = new Card();
    card.setId(json.getInteger("id"));
    card.setUserId(json.getInteger("user_id"));
    card.setCardNumber(json.getString("card_number"));
    card.setCardType(json.getString("card_type"));
    card.setExpireDate(json.getString("expire_date"));
    card.setCvv(json.getString("cvv"));
    card.setCardProvider(json.getString("card_provider"));

    card.setCreatedAt(parseTimestamp(json, "created_at"));
    card.setUpdatedAt(parseTimestamp(json, "updated_at"));
    card.setDeletedAt(parseTimestamp(json, "deleted_at"));

    return card;
  }

  public static Card fromRow(Row row) {
    if (row == null)
      return null;

    // cards PK is "card_id" (V5 migration); repositories alias it as "id".
    // row.getInteger throws NoSuchElementException when a column is absent, not null,
    // so each lookup needs its own guard.
    Integer id = readId(row);
    Integer userId = row.getInteger("user_id");
    String cardNumber = row.getString("card_number");
    String cardType = row.getString("card_type");
    String expireDate = readExpireDate(row);
    String cvv = row.getString("cvv");
    String cardProvider = row.getString("card_provider");

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

    return Card.builder()
        .id(id)
        .userId(userId)
        .cardNumber(cardNumber)
        .cardType(cardType)
        .expireDate(expireDate)
        .cvv(cvv)
        .cardProvider(cardProvider)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .deletedAt(deletedAt)
        .build();
  }

  /**
   * cards.expire_date is a DATE column (see V5 migration), so the pg driver decodes it
   * to {@link LocalDate}. Calling row.getString() on it throws ClassCastException.
   * The model keeps expireDate as an ISO-8601 String, so decode as LocalDate first and
   * fall back to getString() for queries that already cast the column to text.
   */
  static Integer readId(Row row) {
    try {
      Integer id = row.getInteger("id");
      if (id != null) {
        return id;
      }
    } catch (NoSuchElementException ignored) {
      // query did not alias card_id AS id
    }
    try {
      return row.getInteger("card_id");
    } catch (NoSuchElementException ignored) {
      return null;
    }
  }

  static String readExpireDate(Row row) {
    try {
      LocalDate date = row.getLocalDate("expire_date");
      if (date != null) {
        return date.toString();
      }
    } catch (ClassCastException ignored) {
      // column already text/varchar, fall through to getString below
    }
    return row.getString("expire_date");
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
