package io.example.card.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.card.model.BillingStatement;
import io.example.card.model.CardCreditAccount;
import io.example.card.repository.BillingStatementRepository;
import io.example.card.repository.CardCreditAccountRepository;
import io.example.card.service.BillingEngineService;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.utils.InterestCalculator;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class BillingEngineServiceImpl implements BillingEngineService {
  private static final Logger log = LoggerFactory.getLogger(BillingEngineServiceImpl.class);

  private static final long MINIMUM_PAYMENT_FLOOR = 50_000L; // IDR paise (500 IDR)

  private final CardCreditAccountRepository creditAccountRepo;
  private final BillingStatementRepository statementRepo;
  private final TracingMetrics metrics;
  private final KafkaService kafkaService;

  @Override
  public Future<Integer> triggerBillingCycle(int billingCycleDay) {
    var ctx = metrics.startSpan("BillingEngineService.triggerBillingCycle");

    return creditAccountRepo.findAccountsDueForBilling(billingCycleDay)
        .compose(accounts -> {
          if (accounts.isEmpty()) {
            log.info("No accounts due for billing on cycle day {}", billingCycleDay);
            return Future.succeededFuture(0);
          }

          AtomicInteger generated = new AtomicInteger(0);

          // Process each account sequentially to avoid DB contention
          Future<Void> chain = Future.succeededFuture();
          for (CardCreditAccount account : accounts) {
            chain = chain.compose(v -> generateStatement(account)
                .onSuccess(stmt -> {
                  if (stmt != null) {
                    generated.incrementAndGet();
                    publishStatementGenerated(stmt);
                  }
                })
                .onFailure(err -> log.error("Failed to generate statement for card {}: {}",
                    account.getCardNumber(), err.getMessage()))
                .mapEmpty());
          }

          return chain.map(v -> generated.get());
        })
        .onSuccess(count -> metrics.completeSpanSuccess(ctx, "triggerBillingCycle",
            "Generated " + count + " statements"))
        .onFailure(e -> metrics.completeSpanError(ctx, "triggerBillingCycle", e.getMessage()));
  }

  @Override
  public Future<BillingStatement> getStatement(String cardNumber, LocalDate statementDate) {
    // A null statementDate means "latest statement" (the handler treats a
    // blank statement_date query param as optional).
    if (statementDate == null) {
      return statementRepo.findLatestByCardNumber(cardNumber);
    }
    return statementRepo.findByCardAndCycle(cardNumber, statementDate);
  }

  @Override
  public Future<List<BillingStatement>> getStatementsByCard(String cardNumber, int page, int pageSize) {
    int offset = (page - 1) * pageSize;
    return statementRepo.findByCardNumber(cardNumber, pageSize, offset);
  }

  private Future<BillingStatement> generateStatement(CardCreditAccount account) {
    LocalDate today = LocalDate.now();
    LocalDate lastStatement = account.getLastStatementDate() != null
        ? account.getLastStatementDate()
        : today.minusMonths(1);
    int cycleDays = InterestCalculator.daysBetween(lastStatement, today);

    // For simplicity, total used_credit during this cycle
    // In production: aggregate from card_auth_transactions
    long purchases = Math.max(0, account.getUsedCredit() - (account.getLastStatementDate() != null
        ? account.getUsedCredit() : 0));
    // Use current used_credit as rough opening balance
    long openingBalance = account.getLastStatementDate() != null ? account.getUsedCredit() : 0;
    long cashAdvances = 0; // Would need separate tracking
    long fees = 0;
    long payments = 0;

    // Interest calculation
    long interestCharged = InterestCalculator.calculateDailyBalanceInterest(
        account.getAnnualRateBps(), openingBalance, purchases, payments, cycleDays);

    long closingBalance = openingBalance + purchases + cashAdvances + fees + interestCharged - payments;
    long minimumPayment = InterestCalculator.calculateMinimumPayment(closingBalance, MINIMUM_PAYMENT_FLOOR);

    LocalDate dueDate = today.plusDays(account.getPaymentDueDays());

    BillingStatement stmt = BillingStatement.builder()
        .cardNumber(account.getCardNumber())
        .statementDate(today)
        .dueDate(dueDate)
        .openingBalance(openingBalance)
        .purchases(purchases)
        .cashAdvances(cashAdvances)
        .payments(payments)
        .fees(fees)
        .interestCharged(interestCharged)
        .closingBalance(closingBalance)
        .minimumPayment(minimumPayment)
        .paymentStatus("UNPAID")
        .build();

    return statementRepo.insertStatement(stmt)
        .compose(inserted -> {
          // Update account's last statement date
          return creditAccountRepo.findByCardNumber(account.getCardNumber())
              .compose(updated -> {
                // In production: UPDATE last_statement_date, next_statement_date on the account
                return Future.succeededFuture(inserted);
              });
        });
  }

  private void publishStatementGenerated(BillingStatement stmt) {
    try {
      JsonObject payload = new JsonObject()
          .put("statement_id", stmt.getStatementId())
          .put("card_number", stmt.getCardNumber())
          .put("statement_date", stmt.getStatementDate() != null ? stmt.getStatementDate().toString() : null)
          .put("due_date", stmt.getDueDate() != null ? stmt.getDueDate().toString() : null)
          .put("closing_balance", stmt.getClosingBalance())
          .put("minimum_payment", stmt.getMinimumPayment())
          .put("payment_status", stmt.getPaymentStatus());

      kafkaService.sendMessage("card.statement.generated",
          stmt.getCardNumber() + ":" + stmt.getStatementDate(), payload);
    } catch (Exception e) {
      log.warn("Failed to publish statement.generated event: {}", e.getMessage());
    }
  }
}
