package io.example.card.handler;

import io.example.card.service.CreditLimitService;
import io.example.common.grpc.GrpcExceptionMapper;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.card.CardLimit;
import pb.card.VertxCardLimitServiceGrpcServer;

@RequiredArgsConstructor
public class CardLimitHandler implements VertxCardLimitServiceGrpcServer.CardLimitServiceApi {
  private final CreditLimitService service;

  @Override
  public Future<CardLimit.GetLimitResponse> getLimit(CardLimit.GetLimitByCardNumberRequest req) {
    return service.getLimit(req.getCardNumber())
        .map(account -> {
          var result = toLimitResult(account);
          return CardLimit.GetLimitResponse.newBuilder()
              .setStatus("success")
              .setMessage("OK")
              .setData(result)
              .build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<CardLimit.SetLimitResponse> setLimit(CardLimit.SetLimitRequest req) {
    return service.setLimit(
            req.getCardNumber(),
            req.getCreditLimit(),
            req.getBillingCycleDay() != 0 ? req.getBillingCycleDay() : null,
            req.getAnnualRateBps() != 0 ? req.getAnnualRateBps() : null)
        .map(account -> {
          var result = toLimitResult(account);
          return CardLimit.SetLimitResponse.newBuilder()
              .setStatus("success")
              .setMessage("OK")
              .setData(result)
              .build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<CardLimit.GetLimitResponse> adjustLimit(CardLimit.AdjustLimitRequest req) {
    return service.adjustLimit(req.getCardNumber(), req.getDelta())
        .map(account -> {
          var result = toLimitResult(account);
          return CardLimit.GetLimitResponse.newBuilder()
              .setStatus("success")
              .setMessage("OK")
              .setData(result)
              .build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  private CardLimit.CreditLimitResult toLimitResult(io.example.card.model.CardCreditAccount account) {
    return CardLimit.CreditLimitResult.newBuilder()
        .setCardNumber(account.getCardNumber())
        .setCreditLimit(account.getCreditLimit())
        .setUsedCredit(account.getUsedCredit())
        .setAvailableCredit(account.getAvailableCredit() != null ? account.getAvailableCredit() : 0)
        .setStatus(account.getStatus())
        .setBillingCycleDay(account.getBillingCycleDay())
        .setAnnualRateBps(account.getAnnualRateBps())
        .build();
  }
}
