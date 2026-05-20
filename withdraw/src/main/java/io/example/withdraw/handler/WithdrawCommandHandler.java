package io.example.withdraw.handler;

import io.example.withdraw.service.WithdrawCommandService;
import io.vertx.core.Future;
import pb.withdraw.Withdraw.*;
import pb.withdraw.WithdrawCommand.*;

public class WithdrawCommandHandler implements pb.withdraw.VertxWithdrawCommandServiceGrpcServer.WithdrawCommandServiceApi {
  private final WithdrawCommandService service;

  public WithdrawCommandHandler(WithdrawCommandService service) {
    this.service = service;
  }

  @Override
  public Future<ApiResponseWithdraw> createWithdraw(CreateWithdrawRequest req) {
    return service.createWithdraw(req)
        .map(resp -> {
          var builder = ApiResponseWithdraw.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromWithdrawResponse(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseWithdraw> updateWithdraw(UpdateWithdrawRequest req) {
    return service.updateWithdraw(req)
        .map(resp -> {
          var builder = ApiResponseWithdraw.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromWithdrawResponse(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApIResponseWithdrawDeleteAt> trashedWithdraw(FindByIdWithdrawRequest req) {
    return service.trashWithdraw(req.getWithdrawId())
        .map(resp -> {
          var builder = ApIResponseWithdrawDeleteAt.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromWithdrawResponseDeleteAt(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApIResponseWithdrawDeleteAt> restoreWithdraw(FindByIdWithdrawRequest req) {
    return service.restoreWithdraw(req.getWithdrawId())
        .map(resp -> {
          var builder = ApIResponseWithdrawDeleteAt.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromWithdrawResponseDeleteAt(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseWithdrawDelete> deleteWithdrawPermanent(FindByIdWithdrawRequest req) {
    return service.deleteWithdrawPermanently(req.getWithdrawId())
        .map(resp -> ApiResponseWithdrawDelete.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .build());
  }

  @Override
  public Future<ApiResponseWithdrawAll> restoreAllWithdraw(com.google.protobuf.Empty req) {
    return service.restoreAllWithdraws()
        .map(resp -> ApiResponseWithdrawAll.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .build());
  }

  @Override
  public Future<ApiResponseWithdrawAll> deleteAllWithdrawPermanent(com.google.protobuf.Empty req) {
    return service.deleteAllPermanentWithdraws()
        .map(resp -> ApiResponseWithdrawAll.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .build());
  }
}
