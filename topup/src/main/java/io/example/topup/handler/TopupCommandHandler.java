package io.example.topup.handler;

import io.example.topup.service.TopupCommandService;
import io.vertx.core.Future;
import pb.topup.Topup.*;
import pb.topup.TopupCommand.*;

public class TopupCommandHandler implements pb.topup.VertxTopupCommandServiceGrpcServer.TopupCommandServiceApi {
  private final TopupCommandService service;

  public TopupCommandHandler(TopupCommandService service) {
    this.service = service;
  }

  @Override
  public Future<ApiResponseTopup> createTopup(CreateTopupRequest req) {
    return service.createTopup(req)
        .map(resp -> {
          var builder = ApiResponseTopup.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromTopupResponse(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseTopup> updateTopup(UpdateTopupRequest req) {
    return service.updateTopup(req)
        .map(resp -> {
          var builder = ApiResponseTopup.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromTopupResponse(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseTopupDeleteAt> trashedTopup(FindByIdTopupRequest req) {
    return service.trashTopup(req.getTopupId())
        .map(resp -> {
          var builder = ApiResponseTopupDeleteAt.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromTopupResponseDeleteAt(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseTopupDeleteAt> restoreTopup(FindByIdTopupRequest req) {
    return service.restoreTopup(req.getTopupId())
        .map(resp -> {
          var builder = ApiResponseTopupDeleteAt.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromTopupResponseDeleteAt(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseTopupDelete> deleteTopupPermanent(FindByIdTopupRequest req) {
    return service.deleteTopupPermanently(req.getTopupId())
        .map(resp -> ApiResponseTopupDelete.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .build());
  }

  @Override
  public Future<ApiResponseTopupAll> restoreAllTopup(com.google.protobuf.Empty req) {
    return service.restoreAllTopups()
        .map(resp -> ApiResponseTopupAll.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .build());
  }

  @Override
  public Future<ApiResponseTopupAll> deleteAllTopupPermanent(com.google.protobuf.Empty req) {
    return service.deleteAllPermanentTopups()
        .map(resp -> ApiResponseTopupAll.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .build());
  }
}
