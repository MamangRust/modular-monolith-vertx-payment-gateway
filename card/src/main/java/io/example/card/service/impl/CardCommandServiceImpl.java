package io.example.card.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.card.model.Card;
import io.example.card.repository.CardCommandRepository;
import io.example.card.repository.CardQueryRepository;
import io.example.card.repository.UserClientRepository;
import io.example.card.service.CardCommandService;
import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;
import pb.card.CardCommand.CreateCardRequest;
import pb.card.CardCommand.UpdateCardRequest;

@RequiredArgsConstructor
public class CardCommandServiceImpl implements CardCommandService {
  private static final Logger logger = LoggerFactory.getLogger(CardCommandServiceImpl.class);

  private final CardCommandRepository repository;
  private final CardQueryRepository queryRepo;
  private final UserClientRepository userClient;
  private final RedisService redis;
  private final TracingMetrics metrics;
  private final KafkaService kafkaService;

  @Override
  public Future<Card> createCard(CreateCardRequest request) {
    var ctx = metrics.startSpan("CardCommandService.createCard");
    return userClient.getUserById(request.getUserId())
        .compose(user -> repository.createCard(request))
        .compose(card -> {
          JsonObject saldoPayload = new JsonObject()
              .put("card_number", card.getCardNumber())
              .put("total_balance", 0);

          return kafkaService
              .sendMessage("saldo-service-topic-create-saldo", String.valueOf(card.getId()), saldoPayload)
              .map(v -> card);
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "createCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "createCard", e.getMessage()));
  }

  @Override
  public Future<Card> updateCard(UpdateCardRequest request) {
    var ctx = metrics.startSpan("CardCommandService.updateCard");
    return userClient.getUserById(request.getUserId())
        .compose(user -> repository.updateCard(request))
        .compose(card -> redis.delete("card:" + card.getId()).map(v -> card))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "updateCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "updateCard", e.getMessage()));
  }

  @Override
  public Future<Card> trashedCard(Integer cardId) {
    var ctx = metrics.startSpan("CardCommandService.trashedCard");
    return repository.trashedCard(cardId)
        .compose(card -> redis.delete("card:" + cardId).map(v -> card))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "trashedCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "trashedCard", e.getMessage()));
  }

  @Override
  public Future<Card> restoreCard(Integer cardId) {
    var ctx = metrics.startSpan("CardCommandService.restoreCard");

    return queryRepo.findByTrashId(cardId)
        .compose(trashed -> {
          if (trashed == null)
            return Future.failedFuture(new BadRequestException("Card not found or must be trashed first"));
          return repository.restoreCard(cardId);
        })
        .compose(card -> redis.delete("card:" + cardId).map(v -> card))
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "restoreCard", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "restoreCard", e.getMessage()));
  }

  @Override
  public Future<Void> deleteCardPermanent(Integer cardId) {
    var ctx = metrics.startSpan("CardCommandService.deleteCardPermanent",
        Attributes.builder().put("card.id", cardId).build());

    return queryRepo.findByTrashId(cardId)
        .compose(trashed -> {
          if (trashed == null)
            return Future.failedFuture(new BadRequestException("Card not found or must be trashed first"));
          return repository.deleteCardPermanent(cardId);
        })
        .compose(deleted -> {
          if (!deleted)
            return Future.failedFuture(new BadRequestException("Card not found or must be trashed first"));
          return redis.delete("card:" + cardId).map(v -> (Void) null);
        })
        .onSuccess(v -> metrics.completeSpanSuccess(ctx, "deleteCardPermanent", "Success"))
        .onFailure(e -> {
          logger.error("Failed to deletePermanent card: {}", cardId, e);
          metrics.completeSpanError(ctx, "deleteCardPermanent", e.getMessage());
        });
  }

  @Override
  public Future<Void> restoreAllCard() {
    var ctx = metrics.startSpan("CardCommandService.restoreAllCard");
    return repository.restoreAllCards()
        .compose(count -> {
          if (count == 0)
            return Future.<Void>failedFuture(new NotFoundException("No trashed cards found"));
          return Future.succeededFuture();
        })
        .onSuccess(v -> metrics.completeSpanSuccess(ctx, "restoreAllCard", "Success"))
        .onFailure(e -> {
          logger.error("Failed to restore all cards", e);
          metrics.completeSpanError(ctx, "restoreAllCard", e.getMessage());
        });
  }

  @Override
  public Future<Void> deleteAllCardPermanent() {
    var ctx = metrics.startSpan("CardCommandService.deleteAllCardPermanent");
    return repository.deleteAllCardsPermanent()
        .compose(count -> {
          if (count == 0)
            return Future.<Void>failedFuture(new NotFoundException("No trashed cards found"));
          return Future.succeededFuture();
        })
        .onSuccess(v -> metrics.completeSpanSuccess(ctx, "deleteAllCardPermanent", "Success"))
        .onFailure(e -> {
          logger.error("Failed to permanently delete all cards", e);
          metrics.completeSpanError(ctx, "deleteAllCardPermanent", e.getMessage());
        });
  }
}