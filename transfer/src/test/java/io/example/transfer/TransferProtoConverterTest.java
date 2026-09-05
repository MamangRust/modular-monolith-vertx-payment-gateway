package io.example.transfer;

import io.example.transfer.handler.ProtoConverter;
import io.example.transfer.model.Transfer;
import io.example.transfer.model.TransferResponse;
import io.example.transfer.model.TransferResponseDeleteAt;
import io.example.transfer.model.TransferStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TransferProtoConverterTest {

  private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 6, 26, 10, 0, 0, 0, ZoneOffset.UTC);

  /* ───────── toResponse tests ───────── */

  @Test
  @DisplayName("null Transfer returns default instance")
  void nullInputReturnsDefault() {
    var result = ProtoConverter.toResponse(null);
    assertThat(result.getId()).isZero();
    assertThat(result.getTransferNo()).isEmpty();
    assertThat(result.getTransferAmount()).isZero();
  }

  @Test
  @DisplayName("maps all fields from Transfer to TransferResponse")
  void mapsAllFields() {
    var t = Transfer.builder()
        .id(1)
        .transferNo("TRF-001")
        .transferFrom("4111111111111111")
        .transferTo("5555555555554444")
        .transferAmount(200_000L)
        .transferTime(NOW)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build();

    var result = ProtoConverter.toResponse(t);

    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.getTransferNo()).isEqualTo("TRF-001");
    assertThat(result.getTransferFrom()).isEqualTo("4111111111111111");
    assertThat(result.getTransferTo()).isEqualTo("5555555555554444");
    assertThat(result.getTransferAmount()).isEqualTo(200_000);
    assertThat(result.getTransferTime()).isEqualTo(NOW.toString());
    assertThat(result.getCreatedAt()).isEqualTo(NOW.toString());
    assertThat(result.getUpdatedAt()).isEqualTo(NOW.toString());
  }

  @Test
  @DisplayName("null fields default to zero/empty")
  void nullFieldsDefault() {
    var result = ProtoConverter.toResponse(new Transfer());

    assertThat(result.getId()).isZero();
    assertThat(result.getTransferNo()).isEmpty();
    assertThat(result.getTransferAmount()).isZero();
  }

  /* ───────── toResponseDeleted tests ───────── */

  @Test
  @DisplayName("toResponseDeleted includes deletedAt when present")
  void deleteAtIncludesDeleted() {
    var t = Transfer.builder().id(2).deletedAt(NOW).build();
    var result = ProtoConverter.toResponseDeleted(t);

    assertThat(result.hasDeletedAt()).isTrue();
    assertThat(result.getDeletedAt().getValue()).isEqualTo(NOW.toString());
  }

  @Test
  @DisplayName("toResponseDeleted omits deletedAt when null")
  void deleteAtOmitsDeleted() {
    var result = ProtoConverter.toResponseDeleted(Transfer.builder().id(3).build());
    assertThat(result.hasDeletedAt()).isFalse();
  }

  /* ───────── fromTransferResponse tests ───────── */

  @Test
  @DisplayName("fromTransferResponse maps basic fields")
  void fromResponseMaps() {
    var tr = TransferResponse.builder()
        .id(4)
        .transferFrom("4111111111111111")
        .transferTo("55554444")
        .transferAmount(100_000L)
        .createdAt("2026-01-01")
        .updatedAt("2026-06-01")
        .build();

    var result = ProtoConverter.fromTransferResponse(tr);

    assertThat(result.getId()).isEqualTo(4);
    assertThat(result.getTransferFrom()).isEqualTo("4111111111111111");
    assertThat(result.getTransferTo()).isEqualTo("55554444");
    assertThat(result.getTransferAmount()).isEqualTo(100_000);
  }

  /* ───────── Stats converter tests ───────── */

  @Test
  @DisplayName("toMonthAmount converts MonthAmount stat")
  void convertsMonthAmount() {
    var src = new TransferStats.MonthAmount("06", 1_000_000L);

    var result = ProtoConverter.toMonthAmount(src);

    assertThat(result.getMonth()).isEqualTo("06");
    assertThat(result.getTotalAmount()).isEqualTo(1_000_000);
  }

  @Test
  @DisplayName("converts MonthStatus to success and failed responses")
  void convertsMonthStatus() {
    var src = new TransferStats.MonthStatus("2026", "06", 10L, 500_000L);

    var success = ProtoConverter.toMonthSuccess(src);
    assertThat(success.getTotalSuccess()).isEqualTo(10);
    assertThat(success.getTotalAmount()).isEqualTo(500_000);

    var failed = ProtoConverter.toMonthFailed(src);
    assertThat(failed.getTotalFailed()).isEqualTo(10);
    assertThat(failed.getTotalAmount()).isEqualTo(500_000);
  }
}
