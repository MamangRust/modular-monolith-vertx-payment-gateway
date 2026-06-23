package io.example.card.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.model.CardStats;
import io.example.card.repository.CardStatsTransferByCardRepository;
import io.example.card.repository.CardStatsTransferRepository;
import io.example.card.service.CardStatsTransferService;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CardStatsTransferServiceImpl implements CardStatsTransferService {
  private final CardStatsTransferRepository repository;
  private final CardStatsTransferByCardRepository byCardRepository;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final Duration CACHE_TTL = Duration.ofMinutes(15);

  @Override
  public Future<List<CardStats.MonthAmount>> getMonthlyTransferAmountSender(int year) {
    var ctx = metrics.startSpan("CardStatsTransferService.getMonthlyTransferAmountSender");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = "stats:transfer:monthly:sender:" + year;

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("card_stats.cache_hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<CardStats.MonthAmount>>() {
              });
              return Future.succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("card_stats.cache_hit", false);
          return repository.getMonthlyTransferAmountSender(year)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTransferAmountSender", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTransferAmountSender", e.getMessage()));
  }

  @Override
  public Future<List<CardStats.MonthAmount>> getMonthlyTransferAmountReceiver(int year) {
    var ctx = metrics.startSpan("CardStatsTransferService.getMonthlyTransferAmountReceiver");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = "stats:transfer:monthly:receiver:" + year;

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("card_stats.cache_hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<CardStats.MonthAmount>>() {
              });
              return Future.succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("card_stats.cache_hit", false);
          return repository.getMonthlyTransferAmountReceiver(year)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTransferAmountReceiver", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTransferAmountReceiver", e.getMessage()));
  }

  @Override
  public Future<List<CardStats.YearAmount>> getYearlyTransferAmountSender(int endYear) {
    var ctx = metrics.startSpan("CardStatsTransferService.getYearlyTransferAmountSender");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = "stats:transfer:yearly:sender:" + endYear;

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("card_stats.cache_hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<CardStats.YearAmount>>() {
              });
              return Future.succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("card_stats.cache_hit", false);
          return repository.getYearlyTransferAmountSender(endYear)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTransferAmountSender", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTransferAmountSender", e.getMessage()));
  }

  @Override
  public Future<List<CardStats.YearAmount>> getYearlyTransferAmountReceiver(int endYear) {
    var ctx = metrics.startSpan("CardStatsTransferService.getYearlyTransferAmountReceiver");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = "stats:transfer:yearly:receiver:" + endYear;

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("card_stats.cache_hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<CardStats.YearAmount>>() {
              });
              return Future.succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("card_stats.cache_hit", false);
          return repository.getYearlyTransferAmountReceiver(endYear)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTransferAmountReceiver", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTransferAmountReceiver", e.getMessage()));
  }

  @Override
  public Future<List<CardStats.MonthAmount>> getMonthlyTransferAmountBySender(MonthYearCardNumberCard req) {
    var ctx = metrics.startSpan("CardStatsTransferService.getMonthlyTransferAmountBySender");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = "stats:transfer:monthly:sender:" + req.getYear() + ":" + req.getCardNumber();

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("card_stats.cache_hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<CardStats.MonthAmount>>() {
              });
              return Future.succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("card_stats.cache_hit", false);
          return byCardRepository.getMonthlyTransferAmountBySender(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTransferAmountBySender", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTransferAmountBySender", e.getMessage()));
  }

  @Override
  public Future<List<CardStats.MonthAmount>> getMonthlyTransferAmountByReceiver(MonthYearCardNumberCard req) {
    var ctx = metrics.startSpan("CardStatsTransferService.getMonthlyTransferAmountByReceiver");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = "stats:transfer:monthly:receiver:" + req.getYear() + ":" + req.getCardNumber();

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("card_stats.cache_hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<CardStats.MonthAmount>>() {
              });
              return Future.succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("card_stats.cache_hit", false);
          return byCardRepository.getMonthlyTransferAmountByReceiver(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getMonthlyTransferAmountByReceiver", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getMonthlyTransferAmountByReceiver", e.getMessage()));
  }

  @Override
  public Future<List<CardStats.YearAmount>> getYearlyTransferAmountBySender(MonthYearCardNumberCard req) {
    var ctx = metrics.startSpan("CardStatsTransferService.getYearlyTransferAmountBySender");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = "stats:transfer:yearly:sender:" + req.getYear() + ":" + req.getCardNumber();

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("card_stats.cache_hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<CardStats.YearAmount>>() {
              });
              return Future.succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("card_stats.cache_hit", false);
          return byCardRepository.getYearlyTransferAmountBySender(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTransferAmountBySender", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTransferAmountBySender", e.getMessage()));
  }

  @Override
  public Future<List<CardStats.YearAmount>> getYearlyTransferAmountByReceiver(MonthYearCardNumberCard req) {
    var ctx = metrics.startSpan("CardStatsTransferService.getYearlyTransferAmountByReceiver");
    Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
    String cacheKey = "stats:transfer:yearly:receiver:" + req.getYear() + ":" + req.getCardNumber();

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              span.setAttribute("card_stats.cache_hit", true);
              var list = mapper.readValue(jsonStr, new TypeReference<List<CardStats.YearAmount>>() {
              });
              return Future.succeededFuture(list);
            } catch (Exception e) {
              /* fallback */ }
          }
          span.setAttribute("card_stats.cache_hit", false);
          return byCardRepository.getYearlyTransferAmountByReceiver(req)
              .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res));
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getYearlyTransferAmountByReceiver", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getYearlyTransferAmountByReceiver", e.getMessage()));
  }
}