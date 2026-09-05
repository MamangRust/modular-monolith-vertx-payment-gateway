package io.example.card;

import io.example.card.verticle.FraudScoringConsumerVerticle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the pure static risk scoring logic in FraudScoringConsumerVerticle.
 */
class CardFraudScoringTest {

  /* ───────── Amount-based risk ───────── */

  @Test
  @DisplayName("amount > 100,000 IDR adds 30 risk points")
  void veryHighAmountAdds30() {
    int score = FraudScoringConsumerVerticle.computeRiskScore(10_000_001L, null);
    assertThat(score).isGreaterThanOrEqualTo(30);
  }

  @Test
  @DisplayName("amount between 50,000 and 100,000 IDR adds 15 points")
  void highAmountAdds15() {
    int score = FraudScoringConsumerVerticle.computeRiskScore(7_000_000L, null);
    assertThat(score).isEqualTo(15);
  }

  @Test
  @DisplayName("amount between 10,000 and 50,000 IDR adds 5 points")
  void mediumAmountAdds5() {
    int score = FraudScoringConsumerVerticle.computeRiskScore(2_000_000L, null);
    assertThat(score).isEqualTo(5);
  }

  @Test
  @DisplayName("amount <= 10,000 IDR adds no risk")
  void lowAmountAdds0() {
    int score = FraudScoringConsumerVerticle.computeRiskScore(500_000L, null);
    assertThat(score).isZero();
  }

  @Test
  @DisplayName("negative or zero amount adds no risk")
  void zeroAmountAdds0() {
    assertThat(FraudScoringConsumerVerticle.computeRiskScore(0L, null)).isZero();
    assertThat(FraudScoringConsumerVerticle.computeRiskScore(-1000L, null)).isZero();
  }

  /* ───────── MCC-based risk ───────── */

  @Test
  @DisplayName("gambling MCC 7995 adds 40 risk points")
  void gamblingMccAdds40() {
    int score = FraudScoringConsumerVerticle.computeRiskScore(100_000L, "7995");
    assertThat(score).isEqualTo(40);
  }

  @Test
  @DisplayName("crypto MCC 6051 adds 35 risk points")
  void cryptoMccAdds35() {
    int score = FraudScoringConsumerVerticle.computeRiskScore(100_000L, "6051");
    assertThat(score).isEqualTo(35);
  }

  @Test
  @DisplayName("money transfer MCC 4829 adds 20 risk points")
  void moneyTransferMccAdds20() {
    int score = FraudScoringConsumerVerticle.computeRiskScore(100_000L, "4829");
    assertThat(score).isEqualTo(20);
  }

  @Test
  @DisplayName("bars MCC 5813 and pawn shops MCC 5933 add 15 risk points")
  void barsAndPawnAdd15() {
    assertThat(FraudScoringConsumerVerticle.computeRiskScore(100_000L, "5813")).isEqualTo(15);
    assertThat(FraudScoringConsumerVerticle.computeRiskScore(100_000L, "5933")).isEqualTo(15);
  }

  @Test
  @DisplayName("unknown/normal MCC adds no risk on its own")
  void normalMccAdds0() {
    int score = FraudScoringConsumerVerticle.computeRiskScore(100_000L, "5411");
    assertThat(score).isZero();
  }

  @Test
  @DisplayName("null MCC does not throw and adds no MCC risk")
  void nullMccIsSafe() {
    int score = FraudScoringConsumerVerticle.computeRiskScore(100_000L, null);
    assertThat(score).isZero();
  }

  /* ───────── Combined risk ───────── */

  @Test
  @DisplayName("total score is capped at 100")
  void scoreIsCappedAt100() {
    // gambling (40) + crypto (35) + high amount (30) = 105 → capped at 100
    int score = FraudScoringConsumerVerticle.computeRiskScore(15_000_000L, "7995");
    assertThat(score).isEqualTo(70); // 40 + 30 = 70

    // Try to exceed 100
    int score2 = FraudScoringConsumerVerticle.computeRiskScore(15_000_000L, "6051");
    assertThat(score2).isEqualTo(65); // 35 + 30 = 65
  }

  @Test
  @DisplayName("amount and MCC risks accumulate additively")
  void risksAccumulateAdditively() {
    int score = FraudScoringConsumerVerticle.computeRiskScore(7_000_000L, "4829");
    // 15 (high amount) + 20 (money transfer) = 35
    assertThat(score).isEqualTo(35);
  }
}
