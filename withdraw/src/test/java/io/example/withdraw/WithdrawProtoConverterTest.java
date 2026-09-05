package io.example.withdraw;

import io.example.withdraw.handler.ProtoConverter;
import io.example.withdraw.model.Withdraw;
import io.example.withdraw.model.WithdrawResponse;
import io.example.withdraw.model.WithdrawResponseDeleteAt;
import io.example.withdraw.model.WithdrawStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class WithdrawProtoConverterTest {

  private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 6, 26, 10, 0, 0, 0, ZoneOffset.UTC);

  /* ───────── toResponse tests ───────── */

  @Test
  @DisplayName("null Withdraw returns default instance")
  void nullInputReturnsDefault() {
    var result = ProtoConverter.toResponse(null);
    assertThat(result.getWithdrawId()).isZero();
    assertThat(result.getWithdrawNo()).isEmpty();
    assertThat(result.getWithdrawAmount()).isZero();
  }

  @Test
  @DisplayName("maps all fields from Withdraw to WithdrawResponse")
  void mapsAllFields() {
    var w = Withdraw.builder()
        .id(1)
        .withdrawNo("WD-001")
        .cardNumber("4111111111111111")
        .withdrawAmount(200_000L)
        .withdrawTime(NOW)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build();

    var result = ProtoConverter.toResponse(w);

    assertThat(result.getWithdrawId()).isEqualTo(1);
    assertThat(result.getWithdrawNo()).isEqualTo("WD-001");
    assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
    assertThat(result.getWithdrawAmount()).isEqualTo(200_000);
    assertThat(result.getWithdrawTime()).isEqualTo(NOW.toString());
    assertThat(result.getCreatedAt()).isEqualTo(NOW.toString());
    assertThat(result.getUpdatedAt()).isEqualTo(NOW.toString());
  }

  @Test
  @DisplayName("null fields default to zero/empty")
  void nullFieldsDefault() {
    var result = ProtoConverter.toResponse(new Withdraw());

    assertThat(result.getWithdrawId()).isZero();
    assertThat(result.getWithdrawNo()).isEmpty();
    assertThat(result.getWithdrawAmount()).isZero();
  }

  /* ───────── toResponseDeleted tests ───────── */

  @Test
  @DisplayName("toResponseDeleted includes deletedAt when present")
  void deleteAtIncludesDeleted() {
    var w = Withdraw.builder().id(2).deletedAt(NOW).build();
    var result = ProtoConverter.toResponseDeleted(w);

    assertThat(result.hasDeletedAt()).isTrue();
    assertThat(result.getDeletedAt().getValue()).isEqualTo(NOW.toString());
  }

  @Test
  @DisplayName("toResponseDeleted omits deletedAt when null")
  void deleteAtOmitsDeleted() {
    var result = ProtoConverter.toResponseDeleted(Withdraw.builder().id(3).build());
    assertThat(result.hasDeletedAt()).isFalse();
  }

  /* ───────── fromWithdrawResponse tests ───────── */

  @Test
  @DisplayName("fromWithdrawResponse maps basic fields")
  void fromResponseMaps() {
    var wr = WithdrawResponse.builder()
        .id(4)
        .cardNumber("4111111111111111")
        .withdrawAmount(100_000)
        .withdrawTime("2026-01-01T00:00:00Z")
        .createdAt("2026-01-01T00:00:00Z")
        .updatedAt("2026-06-01T00:00:00Z")
        .build();

    var result = ProtoConverter.fromWithdrawResponse(wr);

    assertThat(result.getWithdrawId()).isEqualTo(4);
    assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
    assertThat(result.getWithdrawAmount()).isEqualTo(100_000);
    assertThat(result.getWithdrawTime()).isEqualTo("2026-01-01T00:00:00Z");
    assertThat(result.getCreatedAt()).isEqualTo("2026-01-01T00:00:00Z");
    assertThat(result.getUpdatedAt()).isEqualTo("2026-06-01T00:00:00Z");
  }

  /* ───────── fromWithdrawResponseDeleteAt tests ───────── */

  @Test
  @DisplayName("fromWithdrawResponseDeleteAt includes deletedAt when present")
  void fromResponseDeleteAtIncludesDeleted() {
    var wrd = WithdrawResponseDeleteAt.builder()
        .id(5)
        .cardNumber("4111111111111111")
        .withdrawAmount(200_000)
        .deletedAt("2026-06-26T10:00:00Z")
        .build();

    var result = ProtoConverter.fromWithdrawResponseDeleteAt(wrd);

    assertThat(result.getWithdrawId()).isEqualTo(5);
    assertThat(result.hasDeletedAt()).isTrue();
    assertThat(result.getDeletedAt().getValue()).isEqualTo("2026-06-26T10:00:00Z");
  }

  @Test
  @DisplayName("fromWithdrawResponseDeleteAt omits deletedAt when null")
  void fromResponseDeleteAtOmitsDeleted() {
    var wrd = WithdrawResponseDeleteAt.builder()
        .id(6)
        .cardNumber("4111111111111111")
        .withdrawAmount(100_000)
        .build();

    var result = ProtoConverter.fromWithdrawResponseDeleteAt(wrd);

    assertThat(result.getWithdrawId()).isEqualTo(6);
    assertThat(result.hasDeletedAt()).isFalse();
  }

  /* ───────── Stats converter tests ───────── */

  @Test
  @DisplayName("toMonthAmount converts MonthAmount stat")
  void convertsMonthAmount() {
    var src = new WithdrawStats.MonthAmount("Jun", 1_000_000L);

    var result = ProtoConverter.toMonthAmount(src);

    assertThat(result.getMonth()).isEqualTo("Jun");
    assertThat(result.getTotalAmount()).isEqualTo(1_000_000);
  }

  @Test
  @DisplayName("toYearlyAmount converts YearAmount stat")
  void convertsYearlyAmount() {
    var src = new WithdrawStats.YearAmount("2026", 2_000_000L);

    var result = ProtoConverter.toYearlyAmount(src);

    assertThat(result.getYear()).isEqualTo("2026");
    assertThat(result.getTotalAmount()).isEqualTo(2_000_000);
  }

  @Test
  @DisplayName("converts MonthStatus to success and failed responses")
  void convertsMonthStatus() {
    var src = new WithdrawStats.MonthStatus("2026", "Jun", 10L, 500_000L);

    var success = ProtoConverter.toMonthSuccess(src);
    assertThat(success.getTotalSuccess()).isEqualTo(10);
    assertThat(success.getTotalAmount()).isEqualTo(500_000);

    var failed = ProtoConverter.toMonthFailed(src);
    assertThat(failed.getTotalFailed()).isEqualTo(10);
    assertThat(failed.getTotalAmount()).isEqualTo(500_000);
  }

  @Test
  @DisplayName("converts YearStatus to success and failed responses")
  void convertsYearStatus() {
    var src = new WithdrawStats.YearStatus("2026", 25L, 1_250_000L);

    var success = ProtoConverter.toYearlySuccess(src);
    assertThat(success.getTotalSuccess()).isEqualTo(25);
    assertThat(success.getTotalAmount()).isEqualTo(1_250_000);

    var failed = ProtoConverter.toYearlyFailed(src);
    assertThat(failed.getTotalFailed()).isEqualTo(25);
    assertThat(failed.getTotalAmount()).isEqualTo(1_250_000);
  }
}
