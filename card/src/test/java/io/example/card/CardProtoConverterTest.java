package io.example.card;

import com.google.protobuf.Timestamp;
import io.example.card.handler.ProtoConverter;
import io.example.card.model.Card;
import io.example.card.model.CardEmail;
import io.example.card.model.CardResponse;
import io.example.card.model.CardResponseDeleteAt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CardProtoConverterTest {

  private static final java.sql.Timestamp NOW = java.sql.Timestamp.from(Instant.parse("2026-06-26T10:00:00Z"));

  /* ───────── toResponse tests ───────── */

  @Test
  @DisplayName("null Card returns default instance")
  void nullCardReturnsDefault() {
    var result = ProtoConverter.toResponse(null);
    assertThat(result.getId()).isZero();
    assertThat(result.getCardNumber()).isEmpty();
  }

  @Test
  @DisplayName("maps all fields from Card to CardResponse")
  void mapsAllFields() {
    var card = Card.builder()
        .id(1)
        .userId(42)
        .cardNumber("4111111111111111")
        .cardType("VISA")
        .expireDate("2028-12")
        .cvv("123")
        .cardProvider("BANK")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build();

    var result = ProtoConverter.toResponse(card);

    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.getUserId()).isEqualTo(42);
    assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
    assertThat(result.getCardType()).isEqualTo("VISA");
    assertThat(result.getExpireDate()).isEqualTo("2028-12");
    assertThat(result.getCvv()).isEqualTo("123");
    assertThat(result.getCardProvider()).isEqualTo("BANK");
    assertThat(result.getCreatedAt()).isEqualTo(NOW.toInstant().toString());
    assertThat(result.getUpdatedAt()).isEqualTo(NOW.toInstant().toString());
  }

  @Test
  @DisplayName("null fields default to zero/empty")
  void nullFieldsDefault() {
    var card = new Card();

    var result = ProtoConverter.toResponse(card);

    assertThat(result.getId()).isZero();
    assertThat(result.getUserId()).isZero();
    assertThat(result.getCardNumber()).isEmpty();
    assertThat(result.getCardType()).isEmpty();
    assertThat(result.getExpireDate()).isEmpty();
    assertThat(result.getCvv()).isEmpty();
    assertThat(result.getCardProvider()).isEmpty();
    assertThat(result.getCreatedAt()).isEmpty();
    assertThat(result.getUpdatedAt()).isEmpty();
  }

  /* ───────── toResponseDeleted tests ───────── */

  @Test
  @DisplayName("toResponseDeleted includes deletedAt when present")
  void deletedIncludesDeletedAt() {
    var card = Card.builder().id(2).deletedAt(NOW).build();

    var result = ProtoConverter.toResponseDeleted(card);

    assertThat(result.getId()).isEqualTo(2);
    assertThat(result.hasDeletedAt()).isTrue();
    assertThat(result.getDeletedAt().getValue()).isEqualTo(NOW.toInstant().toString());
  }

  @Test
  @DisplayName("toResponseDeleted omits deletedAt when null")
  void deletedOmitsDeletedAt() {
    var card = Card.builder().id(3).build();

    var result = ProtoConverter.toResponseDeleted(card);

    assertThat(result.hasDeletedAt()).isFalse();
  }

  /* ───────── toEmailResponse tests ───────── */

  @Test
  @DisplayName("null CardEmail returns default instance")
  void nullEmailReturnsDefault() {
    var result = ProtoConverter.toEmailResponse(null);
    assertThat(result.getId()).isZero();
    assertThat(result.getEmail()).isEmpty();
  }

  @Test
  @DisplayName("toEmailResponse maps all fields including email")
  void mapsEmailFields() {
    var ce = CardEmail.builder()
        .id(5)
        .userId(10)
        .email("test@example.com")
        .cardNumber("5555555555554444")
        .cardType("MASTERCARD")
        .expireDate("2029-06")
        .cvv("999")
        .cardProvider("CHASE")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build();

    var result = ProtoConverter.toEmailResponse(ce);

    assertThat(result.getId()).isEqualTo(5);
    assertThat(result.getUserId()).isEqualTo(10);
    assertThat(result.getEmail()).isEqualTo("test@example.com");
    assertThat(result.getCardNumber()).isEqualTo("5555555555554444");
    assertThat(result.getCardType()).isEqualTo("MASTERCARD");
    assertThat(result.getExpireDate()).isEqualTo("2029-06");
    assertThat(result.getCvv()).isEqualTo("999");
    assertThat(result.getCardProvider()).isEqualTo("CHASE");
  }

  /* ───────── formatExpDate tests ───────── */

  @Test
  @DisplayName("formatExpDate converts protobuf Timestamp to ISO date string")
  void formatsProtoTimestamp() {
    var protoTs = Timestamp.newBuilder()
        .setSeconds(Instant.parse("2028-12-15T00:00:00Z").getEpochSecond())
        .build();

    var result = ProtoConverter.formatExpDate(protoTs);

    assertThat(result).isEqualTo("2028-12-15");
  }

  @Test
  @DisplayName("formatExpDate returns today for null/zero timestamp")
  void returnsTodayForNullZero() {
    var today = java.time.LocalDate.now().toString();

    assertThat(ProtoConverter.formatExpDate(null)).isEqualTo(today);
    assertThat(ProtoConverter.formatExpDate(Timestamp.getDefaultInstance())).isEqualTo(today);
  }

  /* ───────── fromResponse / fromResponseDeleted tests ───────── */

  @Test
  @DisplayName("fromResponse maps CardResponse to protobuf")
  void fromResponseMapsFields() {
    var cr = CardResponse.builder()
        .id(7)
        .userId(3)
        .cardNumber("4111111111111111")
        .cardType("VISA")
        .cvv("321")
        .createdAt("2026-01-01")
        .updatedAt("2026-06-01")
        .build();

    var result = ProtoConverter.fromResponse(cr);

    assertThat(result.getId()).isEqualTo(7);
    assertThat(result.getCvv()).isEqualTo("321");
    assertThat(result.getCreatedAt()).isEqualTo("2026-01-01");
  }

  @Test
  @DisplayName("fromResponseDeleted includes deletedAt StringValue when present")
  void fromResponseDeletedIncludesDeleted() {
    var crda = CardResponseDeleteAt.builder()
        .id(8)
        .deletedAt("2026-06-26T00:00:00Z")
        .build();

    var result = ProtoConverter.fromResponseDeleted(crda);

    assertThat(result.hasDeletedAt()).isTrue();
    assertThat(result.getDeletedAt().getValue()).isEqualTo("2026-06-26T00:00:00Z");
  }
}
