package io.example.card.service;

import io.example.card.model.BillingStatement;
import io.vertx.core.Future;

import java.time.LocalDate;
import java.util.List;

public interface BillingEngineService {
  Future<Integer> triggerBillingCycle(int billingCycleDay);

  Future<BillingStatement> getStatement(String cardNumber, LocalDate statementDate);

  Future<List<BillingStatement>> getStatementsByCard(String cardNumber, int page, int pageSize);
}
