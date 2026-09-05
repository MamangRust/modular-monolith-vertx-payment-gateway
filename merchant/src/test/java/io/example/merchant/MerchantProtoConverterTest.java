package io.example.merchant;

import io.example.merchant.handler.ProtoConverter;
import io.example.merchant.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantProtoConverterTest {

  private static final Timestamp NOW = Timestamp.from(Instant.parse("2026-06-26T10:00:00Z"));

  /* ───────── toMerchantResponse tests ───────── */

  @Test
  @DisplayName("null Merchant returns default instance")
  void nullMerchantReturnsDefault() {
    var result = ProtoConverter.toMerchantResponse(null);
    assertThat(result.getId()).isZero();
    assertThat(result.getName()).isEmpty();
  }

  @Test
  @DisplayName("maps all fields from Merchant to MerchantResponse")
  void mapsAllFields() {
    var m = Merchant.builder()
        .id(1).name("Acme Corp").apiKey("abc123").status("active")
        .userId(42).createdAt(NOW).updatedAt(NOW).build();

    var result = ProtoConverter.toMerchantResponse(m);

    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.getName()).isEqualTo("Acme Corp");
    assertThat(result.getApiKey()).isEqualTo("abc123");
    assertThat(result.getStatus()).isEqualTo("active");
    assertThat(result.getUserId()).isEqualTo(42);
    assertThat(result.getCreatedAt()).isEqualTo(NOW.toInstant().toString());
    assertThat(result.getUpdatedAt()).isEqualTo(NOW.toInstant().toString());
  }

  @Test
  @DisplayName("null fields default to zero/empty in MerchantResponse")
  void nullFieldsDefaultInResponse() {
    var result = ProtoConverter.toMerchantResponse(new Merchant());

    assertThat(result.getId()).isZero();
    assertThat(result.getName()).isEmpty();
    assertThat(result.getApiKey()).isEmpty();
    assertThat(result.getStatus()).isEmpty();
    assertThat(result.getUserId()).isZero();
    assertThat(result.getCreatedAt()).isEmpty();
    assertThat(result.getUpdatedAt()).isEmpty();
  }

  /* ───────── toMerchantDeleteAt tests ───────── */

  @Test
  @DisplayName("toMerchantDeleteAt includes deletedAt when present")
  void deleteAtIncludesDeleted() {
    var m = Merchant.builder().id(2).deletedAt(NOW).build();

    var result = ProtoConverter.toMerchantDeleteAt(m);

    assertThat(result.getId()).isEqualTo(2);
    assertThat(result.hasDeletedAt()).isTrue();
    assertThat(result.getDeletedAt().getValue()).isEqualTo(NOW.toInstant().toString());
  }

  @Test
  @DisplayName("toMerchantDeleteAt omits deletedAt when null")
  void deleteAtOmitsDeleted() {
    var result = ProtoConverter.toMerchantDeleteAt(Merchant.builder().id(3).build());

    assertThat(result.hasDeletedAt()).isFalse();
  }

  /* ───────── toDocumentResponse tests ───────── */

  @Test
  @DisplayName("null MerchantDocument returns default instance")
  void nullDocReturnsDefault() {
    var result = ProtoConverter.toDocumentResponse(null);
    assertThat(result.getDocumentId()).isZero();
  }

  @Test
  @DisplayName("maps all fields from MerchantDocument")
  void mapsDocFields() {
    var doc = MerchantDocument.builder()
        .id(10).merchantId(1).documentType("KTP")
        .documentUrl("https://docs.example.com/ktp.pdf")
        .status("verified").note("Looks good")
        .createdAt(NOW).updatedAt(NOW).build();

    var result = ProtoConverter.toDocumentResponse(doc);

    assertThat(result.getDocumentId()).isEqualTo(10);
    assertThat(result.getMerchantId()).isEqualTo(1);
    assertThat(result.getDocumentType()).isEqualTo("KTP");
    assertThat(result.getDocumentUrl()).isEqualTo("https://docs.example.com/ktp.pdf");
    assertThat(result.getStatus()).isEqualTo("verified");
    assertThat(result.getNote()).isEqualTo("Looks good");
    assertThat(result.getUploadedAt()).isEqualTo(NOW.toInstant().toString());
  }

  @Test
  @DisplayName("null document fields default to empty")
  void nullDocFieldsDefault() {
    var result = ProtoConverter.toDocumentResponse(new MerchantDocument());

    assertThat(result.getDocumentId()).isZero();
    assertThat(result.getDocumentType()).isEmpty();
    assertThat(result.getDocumentUrl()).isEmpty();
    assertThat(result.getStatus()).isEmpty();
    assertThat(result.getNote()).isEmpty();
  }

  /* ───────── toDocumentDeleteAt tests ───────── */

  @Test
  @DisplayName("toDocumentDeleteAt includes deletedAt when present")
  void docDeleteAtIncludesDeleted() {
    var doc = MerchantDocument.builder().id(5).deletedAt(NOW).build();

    var result = ProtoConverter.toDocumentDeleteAt(doc);

    assertThat(result.hasDeletedAt()).isTrue();
    assertThat(result.getDeletedAt().getValue()).isEqualTo(NOW.toInstant().toString());
  }

  @Test
  @DisplayName("toDocumentDeleteAt omits deletedAt when null")
  void docDeleteAtOmitsDeleted() {
    var result = ProtoConverter.toDocumentDeleteAt(MerchantDocument.builder().id(6).build());

    assertThat(result.hasDeletedAt()).isFalse();
  }

  /* ───────── toTxnResponse tests ───────── */

  @Test
  @DisplayName("null MerchantTransactions returns default instance")
  void nullTxnReturnsDefault() {
    var result = ProtoConverter.toTxnResponse(null);
    assertThat(result.getId()).isZero();
    assertThat(result.getAmount()).isZero();
  }

  @Test
  @DisplayName("maps all fields from MerchantTransactions")
  void mapsTxnFields() {
    var txn = MerchantTransactions.builder()
        .transactionId(20).cardNumber("4111111111111111")
        .amount(250_000L).paymentMethod("CREDIT_CARD")
        .merchantId(5).merchantName("Store")
        .transactionTime(NOW).createdAt(NOW).updatedAt(NOW).build();

    var result = ProtoConverter.toTxnResponse(txn);

    assertThat(result.getId()).isEqualTo(20);
    assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
    assertThat(result.getAmount()).isEqualTo(250_000);
    assertThat(result.getPaymentMethod()).isEqualTo("CREDIT_CARD");
    assertThat(result.getMerchantId()).isEqualTo(5);
    assertThat(result.getMerchantName()).isEqualTo("Store");
    assertThat(result.getTransactionTime()).isEqualTo(NOW.toInstant().toString());
  }

  @Test
  @DisplayName("toTxnResponse includes deletedAt when present")
  void txnDeleteAtIncluded() {
    var txn = MerchantTransactions.builder().transactionId(1).deletedAt(NOW).build();

    var result = ProtoConverter.toTxnResponse(txn);

    assertThat(result.hasDeletedAt()).isTrue();
  }

  @Test
  @DisplayName("toTxnResponse converts Long amount to int")
  void txnAmountNarrowedToInt() {
    var txn = MerchantTransactions.builder().transactionId(1).amount(999_999_999L).build();

    var result = ProtoConverter.toTxnResponse(txn);

    assertThat(result.getAmount()).isEqualTo(999_999_999);
  }

  /* ───────── fromDocumentResponse / fromDocumentResponseAt tests ───────── */

  @Test
  @DisplayName("fromDocumentResponse maps MerchantDocumentResponse fields")
  void fromDocResponseMaps() {
    var dr = MerchantDocumentResponse.builder()
        .id(7).merchantId(2).documentType("NPWP")
        .documentUrl("https://docs.example.com/npwp.pdf")
        .status("pending").note("Awaiting review")
        .createdAt("2026-01-01").updatedAt("2026-06-01").build();

    var result = ProtoConverter.fromDocumentResponse(dr);

    assertThat(result.getDocumentId()).isEqualTo(7);
    assertThat(result.getDocumentType()).isEqualTo("NPWP");
    assertThat(result.getStatus()).isEqualTo("pending");
    assertThat(result.getNote()).isEqualTo("Awaiting review");
  }

  @Test
  @DisplayName("fromDocumentResponseAt includes deletedAt")
  void fromDocResponseAtIncludesDeleted() {
    var dr = MerchantDocumentResponseDeleteAt.builder()
        .id(8).deletedAt("2026-06-26T00:00:00Z").build();

    var result = ProtoConverter.fromDocumentResponseAt(dr);

    assertThat(result.hasDeletedAt()).isTrue();
    assertThat(result.getDeletedAt().getValue()).isEqualTo("2026-06-26T00:00:00Z");
  }

  /* ───────── fromMerchantResponse / fromMerchantResponseDeleteAt tests ───────── */

  @Test
  @DisplayName("fromMerchantResponse maps fields")
  void fromMerchantResponseMaps() {
    var mr = MerchantResponse.builder()
        .id(3).name("Biz").apiKey("key_xyz").status("active")
        .userId(10).createdAt("2026-01-01").updatedAt("2026-06-01").build();

    var result = ProtoConverter.fromMerchantResponse(mr);

    assertThat(result.getId()).isEqualTo(3);
    assertThat(result.getName()).isEqualTo("Biz");
    assertThat(result.getApiKey()).isEqualTo("key_xyz");
  }

  @Test
  @DisplayName("fromMerchantResponseDeleteAt includes deletedAt")
  void fromMerchantDeleteAtIncludesDeleted() {
    var mr = MerchantResponseDeleteAt.builder()
        .id(4).deletedAt("2026-06-26T10:00:00Z").build();

    var result = ProtoConverter.fromMerchantResponseDeleteAt(mr);

    assertThat(result.hasDeletedAt()).isTrue();
  }

  @Test
  @DisplayName("fromMerchantResponseDeleteAt omits deletedAt when null")
  void fromMerchantDeleteAtOmitsDeleted() {
    var result = ProtoConverter.fromMerchantResponseDeleteAt(
        MerchantResponseDeleteAt.builder().id(5).build());

    assertThat(result.hasDeletedAt()).isFalse();
  }
}
