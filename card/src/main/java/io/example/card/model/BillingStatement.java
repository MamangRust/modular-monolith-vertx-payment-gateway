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
public class BillingStatement {
  private Integer statementId;
  private String cardNumber;
  private LocalDate statementDate;
  private LocalDate dueDate;
  private Long openingBalance;
  private Long purchases;
  private Long cashAdvances;
  private Long payments;
  private Long fees;
  private Long interestCharged;
  private Long closingBalance;
  private Long minimumPayment;
  private String paymentStatus;
  private Timestamp createdAt;

  public static BillingStatement fromRow(Row row) {
    if (row == null) return null;

    return BillingStatement.builder()
        .statementId(row.getInteger("statement_id"))
        .cardNumber(row.getString("card_number"))
        .statementDate(row.getLocalDate("statement_date"))
        .dueDate(row.getLocalDate("due_date"))
        .openingBalance(row.getLong("opening_balance"))
        .purchases(row.getLong("purchases"))
        .cashAdvances(row.getLong("cash_advances"))
        .payments(row.getLong("payments"))
        .fees(row.getLong("fees"))
        .interestCharged(row.getLong("interest_charged"))
        .closingBalance(row.getLong("closing_balance"))
        .minimumPayment(row.getLong("minimum_payment"))
        .paymentStatus(row.getString("payment_status"))
        .createdAt(toTimestamp(row.getLocalDateTime("created_at")))
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
        .put("statement_id", statementId)
        .put("card_number", cardNumber)
        .put("statement_date", statementDate != null ? statementDate.toString() : null)
        .put("due_date", dueDate != null ? dueDate.toString() : null)
        .put("opening_balance", openingBalance)
        .put("purchases", purchases)
        .put("cash_advances", cashAdvances)
        .put("payments", payments)
        .put("fees", fees)
        .put("interest_charged", interestCharged)
        .put("closing_balance", closingBalance)
        .put("minimum_payment", minimumPayment)
        .put("payment_status", paymentStatus)
        .put("created_at", createdAt != null ? createdAt.toString() : null);
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
