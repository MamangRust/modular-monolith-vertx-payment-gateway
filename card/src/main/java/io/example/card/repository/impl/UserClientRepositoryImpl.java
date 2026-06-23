package io.example.card.repository.impl;

import io.example.card.exception.NotFoundException;
import io.example.card.repository.UserClientRepository;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.user.User;
import pb.user.VertxUserQueryServiceGrpcClient;

@RequiredArgsConstructor
public class UserClientRepositoryImpl implements UserClientRepository {
  private final VertxUserQueryServiceGrpcClient client;

  @Override
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
