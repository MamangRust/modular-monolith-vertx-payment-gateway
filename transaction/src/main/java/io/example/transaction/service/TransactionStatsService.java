package io.example.transaction.service;

import java.util.List;

import io.example.common.observability.TracingMetrics;
import io.example.transaction.model.TransactionStats;
import io.example.transaction.repository.TransactionStatsRepository;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.vertx.core.Future;

public class TransactionStatsService {
  private final TransactionStatsRepository repository;
  private final Tracer tracer;

  public TransactionStatsService(TransactionStatsRepository repository, TracingMetrics tracing) {
    this.repository = repository;
    this.tracer = tracing.getTracer();
  }

  public Future<List<TransactionStats.MonthAmount>> getMonthlyAmounts(int year) {
    Span span = tracer.spanBuilder("TransactionStatsService.getMonthlyAmounts").startSpan();
    return repository.getMonthlyAmounts(year).onComplete(r -> span.end());
  }

  public Future<List<TransactionStats.YearAmount>> getYearlyAmounts(int endYear) {
    Span span = tracer.spanBuilder("TransactionStatsService.getYearlyAmounts").startSpan();
    return repository.getYearlyAmounts(endYear).onComplete(r -> span.end());
  }

  public Future<List<TransactionStats.MonthStatus>> getMonthlyStatus(int year, int month, String status) {
    Span span = tracer.spanBuilder("TransactionStatsService.getMonthlyStatus").startSpan();
    return repository.getMonthlyStatus(year, month, status).onComplete(r -> span.end());
  }

  public Future<List<TransactionStats.YearStatus>> getYearlyStatus(int endYear, String status) {
    Span span = tracer.spanBuilder("TransactionStatsService.getYearlyStatus").startSpan();
    return repository.getYearlyStatus(endYear, status).onComplete(r -> span.end());
  }

  public Future<List<TransactionStats.MonthMethod>> getMonthlyMethods(int year) {
    Span span = tracer.spanBuilder("TransactionStatsService.getMonthlyMethods").startSpan();
    return repository.getMonthlyMethods(year).onComplete(r -> span.end());
  }

  public Future<List<TransactionStats.YearMethod>> getYearlyMethods(int endYear) {
    Span span = tracer.spanBuilder("TransactionStatsService.getYearlyMethods").startSpan();
    return repository.getYearlyMethods(endYear).onComplete(r -> span.end());
  }

  public Future<List<TransactionStats.MonthAmount>> getMonthlyAmountsByCard(String card, int year) {
    Span span = tracer.spanBuilder("TransactionStatsService.getMonthlyAmountsByCard").startSpan();
    return repository.getMonthlyAmountsByCard(card, year).onComplete(r -> span.end());
  }

  public Future<List<TransactionStats.YearAmount>> getYearlyAmountsByCard(String card, int endYear) {
    Span span = tracer.spanBuilder("TransactionStatsService.getYearlyAmountsByCard").startSpan();
    return repository.getYearlyAmountsByCard(card, endYear).onComplete(r -> span.end());
  }

  public Future<List<TransactionStats.MonthStatus>> getMonthlyStatusByCard(String card, int year, int month, String status) {
    Span span = tracer.spanBuilder("TransactionStatsService.getMonthlyStatusByCard").startSpan();
    return repository.getMonthlyStatusByCard(card, year, month, status).onComplete(r -> span.end());
  }

  public Future<List<TransactionStats.YearStatus>> getYearlyStatusByCard(String card, int endYear, String status) {
    Span span = tracer.spanBuilder("TransactionStatsService.getYearlyStatusByCard").startSpan();
    return repository.getYearlyStatusByCard(card, endYear, status).onComplete(r -> span.end());
  }

  public Future<List<TransactionStats.MonthMethod>> getMonthlyMethodsByCard(String card, int year) {
    Span span = tracer.spanBuilder("TransactionStatsService.getMonthlyMethodsByCard").startSpan();
    return repository.getMonthlyMethodsByCard(card, year).onComplete(r -> span.end());
  }

  public Future<List<TransactionStats.YearMethod>> getYearlyMethodsByCard(String card, int endYear) {
    Span span = tracer.spanBuilder("TransactionStatsService.getYearlyMethodsByCard").startSpan();
    return repository.getYearlyMethodsByCard(card, endYear).onComplete(r -> span.end());
  }
}
