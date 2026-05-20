package io.example.card.model;

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
public class CardEmail {
  private Integer id;
  private String email;
  private Integer userId;
  private String cardNumber;
  private String cardType;
  private String expireDate;
  private String cvv;
  private String cardProvider;
  private Timestamp createdAt;
  private Timestamp updatedAt;

  public JsonObject toJson() {
    JsonObject json = new JsonObject()
        .put("id", id)
        .put("email", email)
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

    return json;
  }

  public static CardEmail fromJson(JsonObject json) {
    if (json == null) {
      return null;
    }

    CardEmail cardEmail = new CardEmail();
    cardEmail.setId(json.getInteger("id"));
    cardEmail.setEmail(json.getString("email"));
    cardEmail.setUserId(json.getInteger("user_id"));
    cardEmail.setCardNumber(json.getString("card_number"));
    cardEmail.setCardType(json.getString("card_type"));
    cardEmail.setExpireDate(json.getString("expire_date"));
    cardEmail.setCvv(json.getString("cvv"));
    cardEmail.setCardProvider(json.getString("card_provider"));

    cardEmail.setCreatedAt(parseTimestamp(json, "created_at"));
    cardEmail.setUpdatedAt(parseTimestamp(json, "updated_at"));

    return cardEmail;
  }

  public static CardEmail fromRow(Row row) {
    if (row == null)
      return null;

    Integer id = row.getInteger("id");
    if (id == null) {
      id = row.getInteger("card_id");
    }
    String email = row.getString("email");
    Integer userId = row.getInteger("user_id");
    String cardNumber = row.getString("card_number");
    String cardType = row.getString("card_type");
    String expireDate = row.getString("expire_date");
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

    return CardEmail.builder()
        .id(id)
        .email(email)
        .userId(userId)
        .cardNumber(cardNumber)
        .cardType(cardType)
        .expireDate(expireDate)
        .cvv(cvv)
        .cardProvider(cardProvider)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
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
