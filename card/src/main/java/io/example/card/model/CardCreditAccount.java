package io.example.card.model;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
public class CardCreditAccount {
  private Integer accountId;
  private String cardNumber;
  private Long creditLimit;
  private Long usedCredit;
  private Long availableCredit;
  private Integer billingCycleDay;
  private Integer paymentDueDays;
  private Integer annualRateBps;
  private String status;
  private LocalDate lastStatementDate;
  private LocalDate nextStatementDate;
  private Integer delinquencyBucket;
  private Integer dpd;
  private Timestamp createdAt;
  private Timestamp updatedAt;

  public static CardCreditAccount fromJson(JsonObject json) {
    if (json == null) return null;
    return CardCreditAccount.builder()
        .accountId(json.getInteger("account_id"))
        .cardNumber(json.getString("card_number"))
        .creditLimit(json.getLong("credit_limit"))
        .usedCredit(json.getLong("used_credit"))
        .availableCredit(json.getLong("available_credit"))
        .billingCycleDay(json.getInteger("billing_cycle_day"))
        .paymentDueDays(json.getInteger("payment_due_days"))
        .annualRateBps(json.getInteger("annual_rate_bps"))
        .status(json.getString("status"))
        .lastStatementDate(json.getString("last_statement_date") != null
            ? java.time.LocalDate.parse(json.getString("last_statement_date")) : null)
        .nextStatementDate(json.getString("next_statement_date") != null
            ? java.time.LocalDate.parse(json.getString("next_statement_date")) : null)
        .delinquencyBucket(json.getInteger("delinquency_bucket"))
        .dpd(json.getInteger("dpd"))
        .build();
  }

  public static CardCreditAccount fromRow(Row row) {
    if (row == null) return null;

    return CardCreditAccount.builder()
        .accountId(row.getInteger("account_id"))
        .cardNumber(row.getString("card_number"))
        .creditLimit(row.getLong("credit_limit"))
        .usedCredit(row.getLong("used_credit"))
        .availableCredit(row.getLong("available_credit"))
        .billingCycleDay(row.getInteger("billing_cycle_day"))
        .paymentDueDays(row.getInteger("payment_due_days"))
        .annualRateBps(row.getInteger("annual_rate_bps"))
        .status(row.getString("status"))
        .lastStatementDate(row.getLocalDate("last_statement_date"))
        .nextStatementDate(row.getLocalDate("next_statement_date"))
        .delinquencyBucket(row.getInteger("delinquency_bucket"))
        .dpd(row.getInteger("dpd"))
        .createdAt(toTimestamp(row.getLocalDateTime("created_at")))
        .updatedAt(toTimestamp(row.getLocalDateTime("updated_at")))
        .build();
  }

  /**
   * The Vert.x pg client does not support {@code row.get(Timestamp.class, ...)}
   * (throws UnsupportedOperationException); timestamps must be read as
   * {@link LocalDateTime} and converted.
   */
  private static Timestamp toTimestamp(LocalDateTime localDateTime) {
    return localDateTime != null ? Timestamp.valueOf(localDateTime) : null;
  }

  public JsonObject toJson() {
    return new JsonObject()
        .put("account_id", accountId)
        .put("card_number", cardNumber)
        .put("credit_limit", creditLimit)
        .put("used_credit", usedCredit)
        .put("available_credit", availableCredit)
        .put("billing_cycle_day", billingCycleDay)
        .put("payment_due_days", paymentDueDays)
        .put("annual_rate_bps", annualRateBps)
        .put("status", status)
        .put("last_statement_date", lastStatementDate != null ? lastStatementDate.toString() : null)
        .put("next_statement_date", nextStatementDate != null ? nextStatementDate.toString() : null)
        .put("delinquency_bucket", delinquencyBucket)
        .put("dpd", dpd)
        .put("created_at", createdAt != null ? createdAt.toString() : null)
        .put("updated_at", updatedAt != null ? updatedAt.toString() : null);
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
