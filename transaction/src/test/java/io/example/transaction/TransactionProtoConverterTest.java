package io.example.transaction;

import io.example.transaction.handler.ProtoConverter;
import io.example.transaction.model.Transaction;
import io.example.transaction.model.TransactionResponse;
import io.example.transaction.model.TransactionResponseDeleteAt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionProtoConverterTest {

  private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 6, 26, 10, 0, 0, 0, ZoneOffset.UTC);
  private static final String NOW_ISO = "2026-06-26T10:00:00Z";

  /* ───────── toProto tests ───────── */

  @Test
  @DisplayName("null Transaction returns default instance")
  void nullInputReturnsDefault() {
    var result = ProtoConverter.toProto(null);
    assertThat(result.getId()).isZero();
    assertThat(result.getTransactionNo()).isEmpty();
    assertThat(result.getAmount()).isZero();
  }

  @Test
  @DisplayName("maps all fields from Transaction to protobuf")
  void mapsAllFields() {
    var txn = Transaction.builder()
        .id(1)
        .transactionNo("TXN-001")
        .cardNumber("4111111111111111")
        .amount(1_000_000L)
        .paymentMethod("credit_card")
        .merchantId(42)
        .transactionTime(NOW)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build();

    var result = ProtoConverter.toProto(txn);

    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.getTransactionNo()).isEqualTo("TXN-001");
    assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
    assertThat(result.getAmount()).isEqualTo(1_000_000);
    assertThat(result.getPaymentMethod()).isEqualTo("credit_card");
    assertThat(result.getMerchantId()).isEqualTo(42);
    assertThat(result.getTransactionTime()).isEqualTo(NOW_ISO);
    assertThat(result.getCreatedAt()).isEqualTo(NOW_ISO);
    assertThat(result.getUpdatedAt()).isEqualTo(NOW_ISO);
  }

  @Test
  @DisplayName("null OffsetDateTime fields are omitted")
  void nullFieldsOmitted() {
    var txn = Transaction.builder()
        .id(1)
        .transactionNo("TXN-001")
        .cardNumber("4111111111111111")
        .amount(500_000L)
        .build();

    var result = ProtoConverter.toProto(txn);

    assertThat(result.getTransactionTime()).isEmpty();
    assertThat(result.getCreatedAt()).isEmpty();
    assertThat(result.getUpdatedAt()).isEmpty();
  }

  /* ───────── toProtoDeleteAt tests ───────── */

  @Test
  @DisplayName("toProtoDeleteAt includes deletedAt when present")
  void deleteAtIncludesDeleted() {
    var txn = Transaction.builder().id(2).deletedAt(NOW).build();
    var result = ProtoConverter.toProtoDeleteAt(txn);

    assertThat(result.hasDeletedAt()).isTrue();
    assertThat(result.getDeletedAt().getValue()).isEqualTo(NOW_ISO);
  }

  @Test
  @DisplayName("toProtoDeleteAt omits deletedAt when null")
  void deleteAtOmitsDeleted() {
    var result = ProtoConverter.toProtoDeleteAt(Transaction.builder().id(3).build());
    assertThat(result.hasDeletedAt()).isFalse();
  }

  /* ───────── fromTransactionResponse tests ───────── */

  @Test
  @DisplayName("fromTransactionResponse maps TransactionResponse to protobuf")
  void fromResponseMaps() {
    var tr = TransactionResponse.builder()
        .id(5)
        .cardNumber("55554444")
        .amount(250_000)
        .paymentMethod("bank_transfer")
        .merchantId(10)
        .transactionTime(NOW_ISO)
        .createdAt("2026-01-01")
        .updatedAt("2026-06-01")
        .build();

    var result = ProtoConverter.fromTransactionResponse(tr);

    assertThat(result.getId()).isEqualTo(5);
    assertThat(result.getCardNumber()).isEqualTo("55554444");
    assertThat(result.getAmount()).isEqualTo(250_000);
    assertThat(result.getPaymentMethod()).isEqualTo("bank_transfer");
    assertThat(result.getTransactionTime()).isEqualTo(NOW_ISO);
  }

  /* ───────── fromTransactionResponseDeleteAt tests ───────── */

  @Test
  @DisplayName("fromTransactionResponseDeleteAt includes deletedAt when present")
  void fromDeleteAtIncludesDeleted() {
    var trda = TransactionResponseDeleteAt.builder()
        .id(7)
        .deletedAt("2026-06-26T10:00:00Z")
        .build();

    var result = ProtoConverter.fromTransactionResponseDeleteAt(trda);

    assertThat(result.hasDeletedAt()).isTrue();
    assertThat(result.getDeletedAt().getValue()).isEqualTo("2026-06-26T10:00:00Z");
  }
}
