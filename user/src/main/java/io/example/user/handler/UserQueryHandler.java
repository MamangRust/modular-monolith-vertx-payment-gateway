package io.example.user.handler;

import io.example.user.service.UserQueryService;
import io.vertx.core.Future;
import pb.user.User.*;
import pb.user.UserQuery.*;

public class UserQueryHandler implements pb.user.VertxUserQueryServiceGrpcServer.UserQueryServiceApi {
  private final UserQueryService service;

  public UserQueryHandler(UserQueryService service) {
    this.service = service;
  }

  @Override
  public Future<ApiResponsePaginationUser> findAll(FindAllUserRequest req) {
    return service.getUsers(req)
        .map(res -> {
          ApiResponsePaginationUser.Builder builder = ApiResponsePaginationUser.newBuilder()
              .setStatus(res.status() != null ? res.status() : "error")
              .setMessage(res.message() != null ? res.message() : "");
          
          if (res.data() != null) {
            builder.addAllData(res.data().stream().map(ProtoConverter::toUserResponse).toList());
          }
          
          if (res.pagination() != null) {
            builder.setPaginationMeta(pb.common.PaginationMeta.newBuilder()
                .setCurrentPage(res.pagination().currentPage())
                .setPageSize(res.pagination().pageSize())
                .setTotalPages(res.pagination().totalPages())
                .setTotalRecords(res.pagination().totalRecords())
                .build());
          }
          
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseUser> findById(FindByIdUserRequest req) {
    return service.getUserById(req)
        .map(res -> {
          ApiResponseUser.Builder builder = ApiResponseUser.newBuilder()
              .setStatus(res.status() != null ? res.status() : "error")
              .setMessage(res.message() != null ? res.message() : "");
          
          if (res.data() != null) {
            builder.setData(ProtoConverter.toUserResponse(res.data()));
          }
          
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponsePaginationUserDeleteAt> findByActive(FindAllUserRequest req) {
    return service.getActiveUsers(req)
        .map(res -> {
          ApiResponsePaginationUserDeleteAt.Builder builder = ApiResponsePaginationUserDeleteAt.newBuilder()
              .setStatus(res.status() != null ? res.status() : "error")
              .setMessage(res.message() != null ? res.message() : "");
          
          if (res.data() != null) {
            builder.addAllData(res.data().stream().map(ProtoConverter::toUserDeleteAt).toList());
          }
          
          if (res.pagination() != null) {
            builder.setPaginationMeta(pb.common.PaginationMeta.newBuilder()
                .setCurrentPage(res.pagination().currentPage())
                .setPageSize(res.pagination().pageSize())
                .setTotalPages(res.pagination().totalPages())
                .setTotalRecords(res.pagination().totalRecords())
                .build());
          }
          
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponsePaginationUserDeleteAt> findByTrashed(FindAllUserRequest req) {
    return service.getTrashedUsers(req)
        .map(res -> {
          ApiResponsePaginationUserDeleteAt.Builder builder = ApiResponsePaginationUserDeleteAt.newBuilder()
              .setStatus(res.status() != null ? res.status() : "error")
              .setMessage(res.message() != null ? res.message() : "");
          
          if (res.data() != null) {
            builder.addAllData(res.data().stream().map(ProtoConverter::toUserDeleteAt).toList());
          }
          
          if (res.pagination() != null) {
            builder.setPaginationMeta(pb.common.PaginationMeta.newBuilder()
                .setCurrentPage(res.pagination().currentPage())
                .setPageSize(res.pagination().pageSize())
                .setTotalPages(res.pagination().totalPages())
                .setTotalRecords(res.pagination().totalRecords())
                .build());
          }
          
          return builder.build();
        });
  }
}
