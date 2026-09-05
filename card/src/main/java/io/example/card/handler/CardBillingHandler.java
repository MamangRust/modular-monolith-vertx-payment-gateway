package io.example.card.handler;

import io.example.card.service.BillingEngineService;
import io.example.common.grpc.GrpcExceptionMapper;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.card.CardBilling;
import pb.card.VertxCardBillingServiceGrpcServer;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CardBillingHandler implements VertxCardBillingServiceGrpcServer.CardBillingServiceApi {
  private final BillingEngineService service;

  @Override
  public Future<CardBilling.TriggerBillingResponse> triggerBillingCycle(CardBilling.TriggerBillingRequest req) {
    return service.triggerBillingCycle(req.getBillingCycleDay())
        .map(count -> CardBilling.TriggerBillingResponse.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setStatementsGenerated(count)
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<CardBilling.GetStatementResponse> getStatement(CardBilling.GetStatementRequest req) {
    // statement_date is optional; when absent the latest statement is returned.
    LocalDate statementDate = null;
    if (req.getStatementDate() != null && !req.getStatementDate().isBlank()) {
      try {
        statementDate = LocalDate.parse(req.getStatementDate());
      } catch (DateTimeParseException e) {
        return GrpcExceptionMapper.toFailedFuture(
            new io.example.common.exception.grpc.BadRequestException(
                "Invalid statement_date: " + req.getStatementDate()));
      }
    }
    return service.getStatement(req.getCardNumber(), statementDate)
        .map(stmt -> {
          if (stmt == null) {
            return CardBilling.GetStatementResponse.newBuilder()
                .setStatus("error")
                .setMessage("Statement not found")
                .build();
          }
          var result = CardBilling.StatementResult.newBuilder()
              .setStatementId(stmt.getStatementId())
              .setCardNumber(stmt.getCardNumber())
              .setStatementDate(stmt.getStatementDate() != null ? stmt.getStatementDate().toString() : "")
              .setDueDate(stmt.getDueDate() != null ? stmt.getDueDate().toString() : "")
              .setOpeningBalance(stmt.getOpeningBalance())
              .setPurchases(stmt.getPurchases())
              .setCashAdvances(stmt.getCashAdvances())
              .setPayments(stmt.getPayments())
              .setFees(stmt.getFees())
              .setInterestCharged(stmt.getInterestCharged())
              .setClosingBalance(stmt.getClosingBalance())
              .setMinimumPayment(stmt.getMinimumPayment())
              .setPaymentStatus(stmt.getPaymentStatus())
              .build();

          return CardBilling.GetStatementResponse.newBuilder()
              .setStatus("success")
              .setMessage("OK")
              .setData(result)
              .build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<CardBilling.GetStatementsByCardResponse> getStatementsByCard(CardBilling.GetStatementsByCardRequest req) {
    return service.getStatementsByCard(req.getCardNumber(), req.getPage(), req.getPageSize())
        .map(statements -> {
          var results = statements.stream()
              .map(stmt -> CardBilling.StatementResult.newBuilder()
                  .setStatementId(stmt.getStatementId())
                  .setCardNumber(stmt.getCardNumber())
                  .setStatementDate(stmt.getStatementDate() != null ? stmt.getStatementDate().toString() : "")
                  .setDueDate(stmt.getDueDate() != null ? stmt.getDueDate().toString() : "")
                  .setOpeningBalance(stmt.getOpeningBalance())
                  .setPurchases(stmt.getPurchases())
                  .setCashAdvances(stmt.getCashAdvances())
                  .setPayments(stmt.getPayments())
                  .setFees(stmt.getFees())
                  .setInterestCharged(stmt.getInterestCharged())
                  .setClosingBalance(stmt.getClosingBalance())
                  .setMinimumPayment(stmt.getMinimumPayment())
                  .setPaymentStatus(stmt.getPaymentStatus())
                  .build())
              .collect(Collectors.toList());

          return CardBilling.GetStatementsByCardResponse.newBuilder()
              .setStatus("success")
              .setMessage("OK")
              .addAllData(results)
              .setTotal(results.size())
              .build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}
