package io.example.user.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.user.domain.requests.FindAllUsers;
import io.example.user.service.UserQueryService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.user.User.ApiResponseUser;
import pb.user.User.FindAllUserRequest;
import pb.user.User.FindByIdUserRequest;
import pb.user.UserQuery.ApiResponsePaginationUser;
import pb.user.UserQuery.ApiResponsePaginationUserDeleteAt;

@RequiredArgsConstructor
public class UserQueryHandler implements pb.user.VertxUserQueryServiceGrpcServer.UserQueryServiceApi {
  private final UserQueryService service;

  private FindAllUsers toDomainReq(FindAllUserRequest req) {
    return FindAllUsers.builder()
        .search(req.getSearch())
        .page(req.getPage() > 0 ? req.getPage() : 1)
        .pageSize(req.getPageSize() > 0 ? req.getPageSize() : 10)
        .build();
  }

  private pb.common.PaginationMeta toMeta(int totalRecords, int page, int pageSize) {
    int currentPage = page > 0 ? page : 1;
    int size = pageSize > 0 ? pageSize : 10;
    int totalPages = size > 0 ? (int) Math.ceil((double) totalRecords / size) : 0;
    return pb.common.PaginationMeta.newBuilder()
        .setCurrentPage(currentPage)
        .setPageSize(size)
        .setTotalPages(totalPages)
        .setTotalRecords(totalRecords)
        .build();
  }

  @Override
  public Future<ApiResponsePaginationUser> findAll(FindAllUserRequest req) {
    var domainReq = toDomainReq(req);
    return service.getUsers(domainReq)
        .map(res -> ApiResponsePaginationUser.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(res.getData().stream().map(ProtoConverter::toUserResponse).toList())
            .setPaginationMeta(toMeta(res.getTotalRecords(), domainReq.getPage(), domainReq.getPageSize()))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseUser> findById(FindByIdUserRequest req) {
    return service.getUserById(req.getId())
        .map(res -> ApiResponseUser.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.toUserResponse(res))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsePaginationUserDeleteAt> findByActive(FindAllUserRequest req) {
    var domainReq = toDomainReq(req);
    return service.getActiveUsers(domainReq)
        .map(res -> ApiResponsePaginationUserDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(res.getData().stream().map(ProtoConverter::toUserDeleteAt).toList())
            .setPaginationMeta(toMeta(res.getTotalRecords(), domainReq.getPage(), domainReq.getPageSize()))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsePaginationUserDeleteAt> findByTrashed(FindAllUserRequest req) {
    var domainReq = toDomainReq(req);
    return service.getTrashedUsers(domainReq)
        .map(res -> ApiResponsePaginationUserDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(res.getData().stream().map(ProtoConverter::toUserDeleteAt).toList())
            .setPaginationMeta(toMeta(res.getTotalRecords(), domainReq.getPage(), domainReq.getPageSize()))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}
