package io.example.topup;

import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.example.topup.handler.ProtoConverter;
import io.example.topup.model.Topup;
import io.example.topup.model.TopupResponse;
import io.example.topup.model.TopupResponseDeleteAt;
import io.example.topup.model.TopupStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TopupProtoConverterTest {

  private static final java.sql.Timestamp NOW = java.sql.Timestamp.from(Instant.parse("2026-06-26T10:00:00Z"));
  private static final Timestamp PROTO_NOW = Timestamp.newBuilder()
      .setSeconds(Instant.parse("2026-06-26T10:00:00Z").getEpochSecond())
      .setNanos(0)
      .build();

  /* ───────── toResponse tests ───────── */

  @Test
  @DisplayName("null Topup returns default instance")
  void nullTopupReturnsDefault() {
    var result = ProtoConverter.toResponse(null);
    assertThat(result.getId()).isZero();
    assertThat(result.getCardNumber()).isEmpty();
  }

  @Test
  @DisplayName("maps all fields from Topup to TopupResponse")
  void mapsAllFields() {
    var topup = Topup.builder()
        .id(1)
        .cardNumber("4111111111111111")
        .topupNo("TXN001")
        .topupAmount(50000L)
        .topupMethod("CREDIT_CARD")
        .topupTime(NOW)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build();

    var result = ProtoConverter.toResponse(topup);

    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
    assertThat(result.getTopupNo()).isEqualTo("TXN001");
    assertThat(result.getTopupAmount()).isEqualTo(50000);
    assertThat(result.getTopupMethod()).isEqualTo("CREDIT_CARD");
    assertThat(result.getTopupTime()).isEqualTo(NOW.toString());
    assertThat(result.getCreatedAt()).isEqualTo(NOW.toString());
    assertThat(result.getUpdatedAt()).isEqualTo(NOW.toString());
  }

  @Test
  @DisplayName("null fields default to zero/empty")
  void nullFieldsDefault() {
    var topup = new Topup();

    var result = ProtoConverter.toResponse(topup);

    assertThat(result.getId()).isZero();
    assertThat(result.getCardNumber()).isEmpty();
    assertThat(result.getTopupNo()).isEmpty();
    assertThat(result.getTopupAmount()).isZero();
    assertThat(result.getTopupMethod()).isEmpty();
    assertThat(result.getTopupTime()).isEmpty();
    assertThat(result.getCreatedAt()).isEmpty();
    assertThat(result.getUpdatedAt()).isEmpty();
  }

  /* ───────── toResponseDeleted tests ───────── */

  @Test
  @DisplayName("toResponseDeleted includes deletedAt when present")
  void deletedIncludesDeletedAt() {
    var topup = Topup.builder()
        .id(2)
        .cardNumber("4222222222222222")
        .deletedAt(NOW)
        .build();

    var result = ProtoConverter.toResponseDeleted(topup);

    assertThat(result.getId()).isEqualTo(2);
    assertThat(result.hasDeletedAt()).isTrue();
    assertThat(result.getDeletedAt().getValue()).isEqualTo(NOW.toString());
  }

  @Test
  @DisplayName("toResponseDeleted omits deletedAt when null")
  void deletedOmitsDeletedAt() {
    var topup = Topup.builder().id(3).build();

    var result = ProtoConverter.toResponseDeleted(topup);

    assertThat(result.getCardNumber()).isEmpty();
    assertThat(result.hasDeletedAt()).isFalse();
  }

  /* ───────── fromTopupResponse tests ───────── */

  @Test
  @DisplayName("null TopupResponse returns default instance")
  void nullTopupResponseReturnsDefault() {
    var result = ProtoConverter.fromTopupResponse(null);
    assertThat(result.getId()).isZero();
    assertThat(result.getCardNumber()).isEmpty();
  }

  @Test
  @DisplayName("fromTopupResponse maps all fields")
  void fromTopupResponseMapsFields() {
    var resp = TopupResponse.builder()
        .id(7)
        .cardNumber("4111111111111111")
        .amount(75000)
        .status("success")
        .createdAt("2026-01-01")
        .updatedAt("2026-06-01")
        .build();

    var result = ProtoConverter.fromTopupResponse(resp);

    assertThat(result.getId()).isEqualTo(7);
    assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
    assertThat(result.getTopupAmount()).isEqualTo(75000);
    assertThat(result.getCreatedAt()).isEqualTo("2026-01-01");
  }

  /* ───────── fromTopupResponseToProtoDeleteAt tests ───────── */

  @Test
  @DisplayName("null TopupResponse returns default instance for delete-at")
  void nullTopupResponseToProtoDeleteAtReturnsDefault() {
    var result = ProtoConverter.fromTopupResponseToProtoDeleteAt(null);
    assertThat(result.getId()).isZero();
  }

  @Test
  @DisplayName("fromTopupResponseToProtoDeleteAt maps fields")
  void fromTopupResponseToProtoDeleteAtMapsFields() {
    var resp = TopupResponse.builder()
        .id(8)
        .cardNumber("4333333333333333")
        .amount(30000)
        .createdAt("2026-02-01")
        .updatedAt("2026-03-01")
        .build();

    var result = ProtoConverter.fromTopupResponseToProtoDeleteAt(resp);

    assertThat(result.getId()).isEqualTo(8);
    assertThat(result.getCardNumber()).isEqualTo("4333333333333333");
    assertThat(result.getTopupAmount()).isEqualTo(30000);
  }

  /* ───────── fromTopupResponseDeleteAt tests ───────── */

  @Test
  @DisplayName("null TopupResponseDeleteAt returns default instance")
  void nullTopupResponseDeleteAtReturnsDefault() {
    var result = ProtoConverter.fromTopupResponseDeleteAt(null);
    assertThat(result.getId()).isZero();
  }

  @Test
  @DisplayName("fromTopupResponseDeleteAt includes deletedAt StringValue when present")
  void fromTopupResponseDeleteAtIncludesDeleted() {
    var resp = TopupResponseDeleteAt.builder()
        .id(9)
        .cardNumber("4444444444444444")
        .amount(60000)
        .deletedAt("2026-06-26T00:00:00Z")
        .createdAt("2026-01-01")
        .updatedAt("2026-06-01")
        .build();

    var result = ProtoConverter.fromTopupResponseDeleteAt(resp);

    assertThat(result.getId()).isEqualTo(9);
    assertThat(result.getCardNumber()).isEqualTo("4444444444444444");
    assertThat(result.hasDeletedAt()).isTrue();
    assertThat(result.getDeletedAt().getValue()).isEqualTo("2026-06-26T00:00:00Z");
  }

  @Test
  @DisplayName("fromTopupResponseDeleteAt omits deletedAt when null")
  void fromTopupResponseDeleteAtOmitsDeleted() {
    var resp = TopupResponseDeleteAt.builder()
        .id(10)
        .cardNumber("4555555555555555")
        .amount(20000)
        .build();

    var result = ProtoConverter.fromTopupResponseDeleteAt(resp);

    assertThat(result.hasDeletedAt()).isFalse();
  }

  /* ───────── toMonthAmount tests ───────── */

  @Test
  @DisplayName("toMonthAmount maps fields correctly")
  void toMonthAmountMapsFields() {
    var src = new TopupStats.MonthAmount("Jan", 100L);
    var result = ProtoConverter.toMonthAmount(src);

    assertThat(result.getMonth()).isEqualTo("Jan");
    assertThat(result.getTotalAmount()).isEqualTo(100);
  }

  @Test
  @DisplayName("toMonthAmount null fields default to empty/zero")
  void toMonthAmountNullFields() {
    var src = new TopupStats.MonthAmount();
    var result = ProtoConverter.toMonthAmount(src);

    assertThat(result.getMonth()).isEmpty();
    assertThat(result.getTotalAmount()).isZero();
  }

  /* ───────── toYearlyAmount tests ───────── */

  @Test
  @DisplayName("toYearlyAmount maps fields correctly")
  void toYearlyAmountMapsFields() {
    var src = new TopupStats.YearAmount("2026", 1000L);
    var result = ProtoConverter.toYearlyAmount(src);

    assertThat(result.getYear()).isEqualTo("2026");
    assertThat(result.getTotalAmount()).isEqualTo(1000);
  }

  @Test
  @DisplayName("toYearlyAmount null fields default to empty/zero")
  void toYearlyAmountNullFields() {
    var src = new TopupStats.YearAmount();
    var result = ProtoConverter.toYearlyAmount(src);

    assertThat(result.getYear()).isEmpty();
    assertThat(result.getTotalAmount()).isZero();
  }

  /* ───────── toMonthMethod tests ───────── */

  @Test
  @DisplayName("toMonthMethod maps fields correctly")
  void toMonthMethodMapsFields() {
    var src = new TopupStats.MonthMethod("Jan", "CREDIT_CARD", 10L, 1000L);
    var result = ProtoConverter.toMonthMethod(src);

    assertThat(result.getMonth()).isEqualTo("Jan");
    assertThat(result.getTopupMethod()).isEqualTo("CREDIT_CARD");
    assertThat(result.getTotalTopups()).isEqualTo(10);
    assertThat(result.getTotalAmount()).isEqualTo(1000);
  }

  @Test
  @DisplayName("toMonthMethod null fields default to empty/zero")
  void toMonthMethodNullFields() {
    var src = new TopupStats.MonthMethod();
    var result = ProtoConverter.toMonthMethod(src);

    assertThat(result.getMonth()).isEmpty();
    assertThat(result.getTopupMethod()).isEmpty();
    assertThat(result.getTotalTopups()).isZero();
    assertThat(result.getTotalAmount()).isZero();
  }

  /* ───────── toYearlyMethod tests ───────── */

  @Test
  @DisplayName("toYearlyMethod maps fields correctly")
  void toYearlyMethodMapsFields() {
    var src = new TopupStats.YearMethod("2026", "DEBIT", 5L, 500L);
    var result = ProtoConverter.toYearlyMethod(src);

    assertThat(result.getYear()).isEqualTo("2026");
    assertThat(result.getTopupMethod()).isEqualTo("DEBIT");
    assertThat(result.getTotalTopups()).isEqualTo(5);
    assertThat(result.getTotalAmount()).isEqualTo(500);
  }

  @Test
  @DisplayName("toYearlyMethod null fields default to empty/zero")
  void toYearlyMethodNullFields() {
    var src = new TopupStats.YearMethod();
    var result = ProtoConverter.toYearlyMethod(src);

    assertThat(result.getYear()).isEmpty();
    assertThat(result.getTopupMethod()).isEmpty();
    assertThat(result.getTotalTopups()).isZero();
    assertThat(result.getTotalAmount()).isZero();
  }

  /* ───────── toMonthSuccess tests ───────── */

  @Test
  @DisplayName("toMonthSuccess maps fields correctly")
  void toMonthSuccessMapsFields() {
    var src = new TopupStats.MonthStatus("2026", "Jan", 5L, 500L);
    var result = ProtoConverter.toMonthSuccess(src);

    assertThat(result.getYear()).isEqualTo("2026");
    assertThat(result.getMonth()).isEqualTo("Jan");
    assertThat(result.getTotalSuccess()).isEqualTo(5);
    assertThat(result.getTotalAmount()).isEqualTo(500);
  }

  @Test
  @DisplayName("toMonthSuccess null fields default to empty/zero")
  void toMonthSuccessNullFields() {
    var src = new TopupStats.MonthStatus();
    var result = ProtoConverter.toMonthSuccess(src);

    assertThat(result.getYear()).isEmpty();
    assertThat(result.getMonth()).isEmpty();
    assertThat(result.getTotalSuccess()).isZero();
    assertThat(result.getTotalAmount()).isZero();
  }

  /* ───────── toYearlySuccess tests ───────── */

  @Test
  @DisplayName("toYearlySuccess maps fields correctly")
  void toYearlySuccessMapsFields() {
    var src = new TopupStats.YearStatus("2026", 50L, 5000L);
    var result = ProtoConverter.toYearlySuccess(src);

    assertThat(result.getYear()).isEqualTo("2026");
    assertThat(result.getTotalSuccess()).isEqualTo(50);
    assertThat(result.getTotalAmount()).isEqualTo(5000);
  }

  @Test
  @DisplayName("toYearlySuccess null fields default to empty/zero")
  void toYearlySuccessNullFields() {
    var src = new TopupStats.YearStatus();
    var result = ProtoConverter.toYearlySuccess(src);

    assertThat(result.getYear()).isEmpty();
    assertThat(result.getTotalSuccess()).isZero();
    assertThat(result.getTotalAmount()).isZero();
  }

  /* ───────── toMonthFailed tests ───────── */

  @Test
  @DisplayName("toMonthFailed maps fields correctly")
  void toMonthFailedMapsFields() {
    var src = new TopupStats.MonthStatus("2026", "Jan", 2L, 100L);
    var result = ProtoConverter.toMonthFailed(src);

    assertThat(result.getYear()).isEqualTo("2026");
    assertThat(result.getMonth()).isEqualTo("Jan");
    assertThat(result.getTotalFailed()).isEqualTo(2);
    assertThat(result.getTotalAmount()).isEqualTo(100);
  }

  @Test
  @DisplayName("toMonthFailed null fields default to empty/zero")
  void toMonthFailedNullFields() {
    var src = new TopupStats.MonthStatus();
    var result = ProtoConverter.toMonthFailed(src);

    assertThat(result.getYear()).isEmpty();
    assertThat(result.getMonth()).isEmpty();
    assertThat(result.getTotalFailed()).isZero();
    assertThat(result.getTotalAmount()).isZero();
  }

  /* ───────── toYearlyFailed tests ───────── */

  @Test
  @DisplayName("toYearlyFailed maps fields correctly")
  void toYearlyFailedMapsFields() {
    var src = new TopupStats.YearStatus("2026", 10L, 500L);
    var result = ProtoConverter.toYearlyFailed(src);

    assertThat(result.getYear()).isEqualTo("2026");
    assertThat(result.getTotalFailed()).isEqualTo(10);
    assertThat(result.getTotalAmount()).isEqualTo(500);
  }

  @Test
  @DisplayName("toYearlyFailed null fields default to empty/zero")
  void toYearlyFailedNullFields() {
    var src = new TopupStats.YearStatus();
    var result = ProtoConverter.toYearlyFailed(src);

    assertThat(result.getYear()).isEmpty();
    assertThat(result.getTotalFailed()).isZero();
    assertThat(result.getTotalAmount()).isZero();
  }
}
