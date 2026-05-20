package io.example.card.service.impl;

import io.example.card.model.Card;
import io.example.card.repository.CardCommandRepository;
import io.example.card.repository.UserClientRepository;
import io.example.card.service.CardCommandService;
import io.example.common.domain.ApiResponse;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import pb.card.CardCommand.CreateCardRequest;
import pb.card.CardCommand.UpdateCardRequest;

public class CardCommandServiceImpl implements CardCommandService {
  private final CardCommandRepository repository;
  private final UserClientRepository userClient;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private final KafkaService kafkaService;

  public CardCommandServiceImpl(CardCommandRepository repository, UserClientRepository userClient, RedisService redis,
      TracingMetrics metrics, KafkaService kafkaService) {
    this.repository = repository;
    this.userClient = userClient;
    this.redis = redis;
    this.metrics = metrics;
    this.kafkaService = kafkaService;
  }

  @Override
  public Future<ApiResponse<Card>> createCard(CreateCardRequest request) {
    var ctx = metrics.startSpan("CardCommandService.createCard");
    return userClient.getUserById(request.getUserId())
        .compose(user -> repository.createCard(request))
        .compose(card -> {
          // Send Kafka message to saldo service
          JsonObject saldoPayload = new JsonObject()
              .put("card_number", card.getCardNumber())
              .put("total_balance", 0);

          return kafkaService.sendMessage(
              "saldo-service-topic-create-saldo",
              String.valueOf(card.getId()),
              saldoPayload)
              .map(v -> card);
        })
        .map(card -> ApiResponse.success("Card created successfully", card))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "createCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "createCard", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<Card>> updateCard(UpdateCardRequest request) {
    var ctx = metrics.startSpan("CardCommandService.updateCard");
    return userClient.getUserById(request.getUserId())
        .compose(user -> repository.updateCard(request))
        .compose(card -> {
          // Invalidate cache
          return redis.delete("card:" + card.getId())
              .map(v -> card);
        })
        .map(card -> ApiResponse.success("Card updated successfully", card))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "updateCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "updateCard", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<Card>> trashedCard(Integer cardId) {
    var ctx = metrics.startSpan("CardCommandService.trashedCard");
    return repository.trashedCard(cardId)
        .compose(card -> redis.delete("card:" + cardId).map(v -> card))
        .map(card -> ApiResponse.success("Card trashed successfully", card))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "trashedCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "trashedCard", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<Card>> restoreCard(Integer cardId) {
    var ctx = metrics.startSpan("CardCommandService.restoreCard");
    return repository.restoreCard(cardId)
        .compose(card -> redis.delete("card:" + cardId).map(v -> card))
        .map(card -> ApiResponse.success("Card restored successfully", card))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "restoreCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "restoreCard", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<Boolean>> deleteCardPermanent(Integer cardId) {
    var ctx = metrics.startSpan("CardCommandService.deleteCardPermanent");
    return repository.deleteCardPermanent(cardId)
        .compose(success -> redis.delete("card:" + cardId).map(v -> success))
        .map(success -> ApiResponse.success("Card deleted permanently", success))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "deleteCardPermanent", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "deleteCardPermanent", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<Boolean>> restoreAllCard() {
    var ctx = metrics.startSpan("CardCommandService.restoreAllCard");
    return repository.restoreAllCards()
        .map(success -> ApiResponse.success("All cards restored successfully", success))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "restoreAllCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "restoreAllCard", e.getMessage()));
  }

  @Override
  public Future<ApiResponse<Boolean>> deleteAllCardPermanent() {
    var ctx = metrics.startSpan("CardCommandService.deleteAllCardPermanent");
    return repository.deleteAllCardsPermanent()
        .map(success -> ApiResponse.success("All trashed cards deleted permanently", success))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "deleteAllCardPermanent", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "deleteAllCardPermanent", e.getMessage()));
  }
}
