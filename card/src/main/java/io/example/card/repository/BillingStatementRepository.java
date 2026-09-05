package io.example.card.repository;

import io.example.card.model.BillingStatement;
import io.vertx.core.Future;

import java.time.LocalDate;
import java.util.List;

public interface BillingStatementRepository {
  Future<BillingStatement> insertStatement(BillingStatement stmt);

  Future<BillingStatement> findByCardAndCycle(String cardNumber, LocalDate statementDate);

  Future<List<BillingStatement>> findByCardNumber(String cardNumber, int limit, int offset);

  Future<BillingStatement> updatePaymentStatus(Integer statementId, String paymentStatus);

  Future<BillingStatement> findLatestByCardNumber(String cardNumber);

  Future<Integer> countByCardNumber(String cardNumber);
}
