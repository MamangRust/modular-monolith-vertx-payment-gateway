package io.example.saldo;

import io.example.saldo.handler.ProtoConverter;
import io.example.saldo.model.Saldo;
import io.example.saldo.model.SaldoResponse;
import io.example.saldo.model.SaldoResponseDeleteAt;
import io.example.saldo.model.SaldoStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SaldoProtoConverterTest {

  private static final Timestamp NOW = Timestamp.from(Instant.parse("2026-06-26T10:00:00Z"));

  /* ───────── toSaldoResponse tests ───────── */

  @Test
  @DisplayName("null Saldo returns default instance")
  void nullInputReturnsDefault() {
    var result = ProtoConverter.toSaldoResponse(null);
    assertThat(result.getSaldoId()).isZero();
    assertThat(result.getCardNumber()).isEmpty();
    assertThat(result.getTotalBalance()).isZero();
  }

  @Test
  @DisplayName("maps all fields from Saldo to SaldoResponse")
  void mapsAllFields() {
    var saldo = Saldo.builder()
        .id(1)
        .cardNumber("4111111111111111")
        .totalBalance(1_000_000L)
        .withdrawAmount(500_000L)
        .withdrawTime(NOW)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build();

    var result = ProtoConverter.toSaldoResponse(saldo);

    assertThat(result.getSaldoId()).isEqualTo(1);
    assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
    assertThat(result.getTotalBalance()).isEqualTo(1_000_000);
    assertThat(result.getWithdrawAmount()).isEqualTo(500_000);
    assertThat(result.getWithdrawTime()).isEqualTo(NOW.toString());
    assertThat(result.getCreatedAt()).isEqualTo(NOW.toString());
    assertThat(result.getUpdatedAt()).isEqualTo(NOW.toString());
  }

  @Test
  @DisplayName("null fields default to zero/empty")
  void nullFieldsDefault() {
    var result = ProtoConverter.toSaldoResponse(new Saldo());

    assertThat(result.getSaldoId()).isZero();
    assertThat(result.getCardNumber()).isEmpty();
    assertThat(result.getTotalBalance()).isZero();
    assertThat(result.getWithdrawAmount()).isZero();
  }

  /* ───────── toSaldoDeleteAt tests ───────── */

  @Test
  @DisplayName("toSaldoDeleteAt includes deletedAt when present")
  void deleteAtIncludesDeleted() {
    var saldo = Saldo.builder().id(2).deletedAt(NOW).build();

    var result = ProtoConverter.toSaldoDeleteAt(saldo);

    assertThat(result.getSaldoId()).isEqualTo(2);
    assertThat(result.hasDeletedAt()).isTrue();
    assertThat(result.getDeletedAt().getValue()).isEqualTo(NOW.toString());
  }

  @Test
  @DisplayName("toSaldoDeleteAt omits deletedAt when null")
  void deleteAtOmitsDeleted() {
    var result = ProtoConverter.toSaldoDeleteAt(Saldo.builder().id(3).build());

    assertThat(result.hasDeletedAt()).isFalse();
  }

  /* ───────── fromSaldoResponse tests ───────── */

  @Test
  @DisplayName("fromSaldoResponse maps fields (without withdrawAmount/Time)")
  void fromResponseMaps() {
    var sr = SaldoResponse.builder()
        .id(5)
        .cardNumber("55554444")
        .totalBalance(2_000_000L)
        .createdAt("2026-01-01")
        .updatedAt("2026-06-01")
        .build();

    var result = ProtoConverter.fromSaldoResponse(sr);

    assertThat(result.getSaldoId()).isEqualTo(5);
    assertThat(result.getCardNumber()).isEqualTo("55554444");
    assertThat(result.getTotalBalance()).isEqualTo(2_000_000);
    assertThat(result.getCreatedAt()).isEqualTo("2026-01-01");
    assertThat(result.getUpdatedAt()).isEqualTo("2026-06-01");
  }

  /* ───────── Stats converter tests ───────── */

  @Test
  @DisplayName("toProtoMonthTotal converts MonthTotalBalance")
  void convertsMonthTotal() {
    var src = new SaldoStats.MonthTotalBalance("06", "2026", 500_000L);

    var result = ProtoConverter.toProtoMonthTotal(src);

    assertThat(result.getMonth()).isEqualTo("06");
    assertThat(result.getYear()).isEqualTo("2026");
    assertThat(result.getTotalBalance()).isEqualTo(500_000);
  }

  @Test
  @DisplayName("toProtoYearTotal converts YearTotalBalance")
  void convertsYearTotal() {
    var src = new SaldoStats.YearTotalBalance("2026", 3_000_000L);

    var result = ProtoConverter.toProtoYearTotal(src);

    assertThat(result.getYear()).isEqualTo("2026");
    assertThat(result.getTotalBalance()).isEqualTo(3_000_000);
  }

  @Test
  @DisplayName("toProtoMonthBal converts MonthBalance")
  void convertsMonthBal() {
    var src = new SaldoStats.MonthBalance("06", 1_000_000L);

    var result = ProtoConverter.toProtoMonthBal(src);

    assertThat(result.getMonth()).isEqualTo("06");
    assertThat(result.getTotalBalance()).isEqualTo(1_000_000);
  }

  @Test
  @DisplayName("toProtoYearBal converts YearBalance")
  void convertsYearBal() {
    var src = new SaldoStats.YearBalance("2026", 2_500_000L);

    var result = ProtoConverter.toProtoYearBal(src);

    assertThat(result.getYear()).isEqualTo("2026");
    assertThat(result.getTotalBalance()).isEqualTo(2_500_000);
  }

  @Test
  @DisplayName("stats converters return null-safe defaults for null inner fields")
  void statsConvertersHandleNullFields() {
    var result = ProtoConverter.toProtoMonthTotal(new SaldoStats.MonthTotalBalance(null, null, null));

    assertThat(result.getMonth()).isEmpty();
    assertThat(result.getYear()).isEmpty();
    assertThat(result.getTotalBalance()).isZero();
  }
}
