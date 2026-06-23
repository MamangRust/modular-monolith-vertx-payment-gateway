package io.example.merchant.repository;

import io.vertx.core.Future;
import pb.user.User;
import pb.user.VertxUserQueryServiceGrpcClient;

import io.example.common.exception.grpc.NotFoundException;

public class UserClientRepository {
  private final VertxUserQueryServiceGrpcClient client;

  public UserClientRepository(VertxUserQueryServiceGrpcClient client) {
    this.client = client;
  }

  public Future<Object> getUserById(Integer userId) {
    var req = User.FindByIdUserRequest.newBuilder().setId(userId).build();
    return client.findById(req)
        .compose(resp -> {
          if (resp.getData() != null && resp.getData().getId() != 0) {
            return Future.succeededFuture(resp.getData());
          } else {
            return Future.failedFuture(new NotFoundException("User not found with id: " + userId));
          }
        });
  }
}
