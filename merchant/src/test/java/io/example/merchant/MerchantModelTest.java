package io.example.merchant;

import io.example.merchant.handler.ProtoConverter;
import io.example.merchant.model.Merchant;
import io.example.merchant.model.MerchantDocument;
import io.example.merchant.model.MerchantTransactions;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantModelTest {

  private static final Timestamp NOW = Timestamp.from(Instant.parse("2026-06-26T10:00:00Z"));

  /* ───────── Merchant.parseTimestamp tests ───────── */

  @Test
  @DisplayName("parseTimestamp returns null for null value")
  void parseTimestampNull() {
    var merchant = Merchant.fromJson(new JsonObject().put("id", 1));
    assertThat(merchant.getCreatedAt()).isNull();
  }

  @Test
  @DisplayName("parseTimestamp parses ISO-8601 string correctly")
  void parseTimestampIsoString() {
    var json = new JsonObject()
        .put("id", 1)
        .put("created_at", "2026-06-26T10:00:00Z");

    var merchant = Merchant.fromJson(json);

    assertThat(merchant.getCreatedAt()).isNotNull();
    assertThat(merchant.getCreatedAt().toInstant().toString()).isEqualTo("2026-06-26T10:00:00Z");
  }

  @Test
  @DisplayName("parseTimestamp returns null for blank string")
  void parseTimestampBlankString() {
    var json = new JsonObject()
        .put("id", 1)
        .put("created_at", "");

    var merchant = Merchant.fromJson(json);

    assertThat(merchant.getCreatedAt()).isNull();
  }

  @Test
  @DisplayName("parseTimestamp returns null for invalid string")
  void parseTimestampInvalidString() {
    var json = new JsonObject()
        .put("id", 1)
        .put("created_at", "not-a-timestamp");

    var merchant = Merchant.fromJson(json);

    assertThat(merchant.getCreatedAt()).isNull();
  }

  @Test
  @DisplayName("parseTimestamp handles epoch millis as Number")
  void parseTimestampNumber() {
    var epochMillis = Instant.parse("2026-06-26T10:00:00Z").toEpochMilli();
    var json = new JsonObject()
        .put("id", 1)
        .put("created_at", epochMillis);

    var merchant = Merchant.fromJson(json);

    assertThat(merchant.getCreatedAt()).isNotNull();
    assertThat(merchant.getCreatedAt().getTime()).isEqualTo(epochMillis);
  }

  /* ───────── Merchant.toJson / fromJson round-trip ───────── */

  @Test
  @DisplayName("Merchant toJson/fromJson round-trip preserves all fields")
  void merchantJsonRoundTrip() {
    var original = Merchant.builder()
        .id(1).name("Test Merchant").apiKey("key_abc").userId(42)
        .status("active").createdAt(NOW).updatedAt(NOW).build();

    var json = original.toJson();
    var restored = Merchant.fromJson(json);

    assertThat(restored.getId()).isEqualTo(original.getId());
    assertThat(restored.getName()).isEqualTo(original.getName());
    assertThat(restored.getApiKey()).isEqualTo(original.getApiKey());
    assertThat(restored.getUserId()).isEqualTo(original.getUserId());
    assertThat(restored.getStatus()).isEqualTo(original.getStatus());
    assertThat(restored.getCreatedAt()).isEqualTo(original.getCreatedAt());
    assertThat(restored.getUpdatedAt()).isEqualTo(original.getUpdatedAt());
  }

  @Test
  @DisplayName("Merchant toJson excludes null timestamps")
  void merchantJsonExcludesNullTimestamps() {
    var m = Merchant.builder().id(1).name("No Dates").build();

    var json = m.toJson();

    assertThat(json.containsKey("created_at")).isFalse();
    assertThat(json.containsKey("updated_at")).isFalse();
    assertThat(json.containsKey("deleted_at")).isFalse();
  }

  @Test
  @DisplayName("null JsonObject returns null from fromJson")
  void nullJsonReturnsNull() {
    assertThat(Merchant.fromJson(null)).isNull();
  }

  /* ───────── Merchant.fromRow tests ───────── */

  @Test
  @DisplayName("null Row returns null from fromRow")
  void nullRowReturnsNull() {
    assertThat(Merchant.fromRow(null)).isNull();
  }

  /* ───────── MerchantDocument toJson/fromJson ───────── */

  @Test
  @DisplayName("MerchantDocument toJson/fromJson round-trip")
  void docJsonRoundTrip() {
    var doc = MerchantDocument.builder()
        .id(5).merchantId(1).documentType("KTP")
        .documentUrl("https://docs.test.com/doc.pdf")
        .status("verified").note("OK")
        .createdAt(NOW).updatedAt(NOW).build();

    var json = doc.toJson();
    var restored = MerchantDocument.fromJson(json);

    assertThat(restored.getId()).isEqualTo(doc.getId());
    assertThat(restored.getDocumentType()).isEqualTo(doc.getDocumentType());
    assertThat(restored.getStatus()).isEqualTo(doc.getStatus());
    assertThat(restored.getNote()).isEqualTo(doc.getNote());
  }

  /* ───────── MerchantTransactions toJson/fromJson ───────── */

  @Test
  @DisplayName("MerchantTransactions toJson/fromJson round-trip")
  void txnJsonRoundTrip() {
    var txn = MerchantTransactions.builder()
        .transactionId(10).cardNumber("4111111111111111")
        .amount(500_000L).paymentMethod("CREDIT_CARD")
        .merchantId(3).merchantName("Store")
        .transactionTime(NOW).createdAt(NOW).updatedAt(NOW).build();

    var json = txn.toJson();
    var restored = MerchantTransactions.fromJson(json);

    assertThat(restored.getTransactionId()).isEqualTo(txn.getTransactionId());
    assertThat(restored.getCardNumber()).isEqualTo(txn.getCardNumber());
    assertThat(restored.getAmount()).isEqualTo(txn.getAmount());
    assertThat(restored.getPaymentMethod()).isEqualTo(txn.getPaymentMethod());
    assertThat(restored.getMerchantId()).isEqualTo(txn.getMerchantId());
    assertThat(restored.getTransactionTime()).isEqualTo(txn.getTransactionTime());
  }
}
