package io.example.card.handler;

import io.example.card.service.CardPaymentService;
import io.example.common.grpc.GrpcExceptionMapper;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.card.CardPayment;
import pb.card.VertxCardPaymentServiceGrpcServer;

import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CardPaymentHandler implements VertxCardPaymentServiceGrpcServer.CardPaymentServiceApi {
  private final CardPaymentService service;

  @Override
  public Future<CardPayment.PostPaymentResponse> postPayment(CardPayment.PostPaymentRequest req) {
    return service.postPayment(
            req.getReferenceId(),
            req.getCardNumber(),
            req.getAmount(),
            req.getPaymentChannel(),
            req.getStatementId() != 0 ? req.getStatementId() : null)
        .map(payment -> {
          var result = CardPayment.PaymentResult.newBuilder()
              .setPaymentId(payment.getPaymentId() != null ? payment.getPaymentId().toString() : "")
              .setReferenceId(payment.getReferenceId())
              .setCardNumber(payment.getCardNumber())
              .setAmount(payment.getAmount())
              .setPaymentChannel(payment.getPaymentChannel())
              .setPaymentTime(payment.getPaymentTime() != null ? payment.getPaymentTime().toString() : "")
              .setStatus(payment.getStatus())
              .setStatementId(payment.getStatementId() != null ? payment.getStatementId() : 0)
              .setCreatedAt(payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : "")
              .build();

          return CardPayment.PostPaymentResponse.newBuilder()
              .setStatus("success")
              .setMessage("OK")
              .setData(result)
              .build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<CardPayment.GetPaymentHistoryResponse> getPaymentHistory(CardPayment.GetPaymentHistoryRequest req) {
    return service.getPaymentHistory(req.getCardNumber(), req.getPage(), req.getPageSize())
        .compose(payments ->
            service.countPayments(req.getCardNumber())
                .map(total -> {
                  var entries = payments.stream()
                      .map(p -> CardPayment.PaymentHistoryEntry.newBuilder()
                          .setPaymentId(p.getPaymentId() != null ? p.getPaymentId().toString() : "")
                          .setReferenceId(p.getReferenceId())
                          .setAmount(p.getAmount())
                          .setPaymentChannel(p.getPaymentChannel())
                          .setPaymentTime(p.getPaymentTime() != null ? p.getPaymentTime().toString() : "")
                          .setStatus(p.getStatus())
                          .setStatementId(p.getStatementId() != null ? p.getStatementId() : 0)
                          .build())
                      .collect(Collectors.toList());

                  return CardPayment.GetPaymentHistoryResponse.newBuilder()
                      .setStatus("success")
                      .setMessage("OK")
                      .addAllData(entries)
                      .setTotal(total)
                      .build();
                }))
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}
