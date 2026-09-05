package io.example.card.handler;

import io.example.card.service.CardAuthorizationService;
import io.example.common.grpc.GrpcExceptionMapper;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.card.CardAuthorization;
import pb.card.VertxCardAuthorizationServiceGrpcServer;

@RequiredArgsConstructor
public class CardAuthorizationHandler implements VertxCardAuthorizationServiceGrpcServer.CardAuthorizationServiceApi {
  private final CardAuthorizationService service;

  @Override
  public Future<CardAuthorization.AuthorizeResponse> authorize(CardAuthorization.AuthorizeRequest req) {
    return service.authorize(
            req.getCardNumber(),
            req.getMerchantId(),
            req.getAmount(),
            req.getCurrency(),
            req.getPosEntryMode(),
            req.getMcc(),
            req.getIdempotencyKey())
        .map(txn -> {
          var result = CardAuthorization.AuthorizationResult.newBuilder()
              .setTxnId(txn.getTxnId() != null ? txn.getTxnId().toString() : "")
              .setCardNumber(txn.getCardNumber())
              .setAmount(txn.getAmount())
              .setCurrency(txn.getCurrency())
              .setAuthCode(txn.getAuthCode() != null ? txn.getAuthCode() : "")
              .setApprovalStatus(txn.getStatus())
              .setDeclineCode(txn.getDeclineCode() != null ? txn.getDeclineCode() : "")
              .setRiskScore(txn.getRiskScore() != null ? txn.getRiskScore() : 0)
              .build();

          return CardAuthorization.AuthorizeResponse.newBuilder()
              .setStatus("success")
              .setMessage("OK")
              .setData(result)
              .build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<CardAuthorization.ReverseResponse> reverse(CardAuthorization.ReverseRequest req) {
    return service.reverse(req.getTxnId(), req.getCardNumber(), req.getAmount(), req.getIdempotencyKey())
        .map(txn -> CardAuthorization.ReverseResponse.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setReversed("REVERSED".equals(txn.getStatus()))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}
