package io.example.role.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.role.service.RoleQueryService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.role.Role.ApiResponseRole;
import pb.role.Role.ApiResponsesRole;
import pb.role.Role.FindAllRoleRequest;
import pb.role.Role.FindByIdRoleRequest;
import pb.role.Role.FindByIdUserRoleRequest;
import pb.role.RoleQuery.ApiResponsePaginationRole;
import pb.role.RoleQuery.ApiResponsePaginationRoleDeleteAt;

@RequiredArgsConstructor
public class RoleQueryHandler implements pb.role.VertxRoleServiceGrpcServer.RoleServiceApi {
  private final RoleQueryService service;

  private pb.common.PaginationMeta buildPaginationMeta(int page, int pageSize, int totalRecords) {
    int safePage = page > 0 ? page : 1;
    int safePageSize = pageSize > 0 ? pageSize : 10;
    int totalPages = (int) Math.ceil((double) totalRecords / safePageSize);
    return pb.common.PaginationMeta.newBuilder()
        .setCurrentPage(safePage)
        .setPageSize(safePageSize)
        .setTotalPages(totalPages)
        .setTotalRecords(totalRecords)
        .build();
  }

  @Override
  public Future<ApiResponsePaginationRole> findAllRole(FindAllRoleRequest req) {
    io.example.role.domain.requests.FindAllRoles domainReq = io.example.role.domain.requests.FindAllRoles.builder()
        .page(req.getPage())
        .pageSize(req.getPageSize())
        .search(req.getSearch())
        .build();

    return service.getAllRoles(domainReq)
        .map(resp -> ApiResponsePaginationRole.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(resp.getData().stream().map(ProtoConverter::fromRoleResponse).toList())
            .setPaginationMeta(buildPaginationMeta(req.getPage(), req.getPageSize(), resp.getTotalRecords()))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseRole> findByIdRole(FindByIdRoleRequest req) {
    return service.getRoleById(req.getRoleId())
        .map(data -> ApiResponseRole.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromRoleResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsePaginationRoleDeleteAt> findByActive(FindAllRoleRequest req) {
    io.example.role.domain.requests.FindAllRoles domainReq = io.example.role.domain.requests.FindAllRoles.builder()
        .page(req.getPage())
        .pageSize(req.getPageSize())
        .search(req.getSearch())
        .build();

    return service.getActiveRoles(domainReq)
        .map(resp -> ApiResponsePaginationRoleDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(resp.getData().stream().map(ProtoConverter::fromRoleResponseDeleteAt).toList())
            .setPaginationMeta(buildPaginationMeta(req.getPage(), req.getPageSize(), resp.getTotalRecords()))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsePaginationRoleDeleteAt> findByTrashed(FindAllRoleRequest req) {
    io.example.role.domain.requests.FindAllRoles domainReq = io.example.role.domain.requests.FindAllRoles.builder()
        .page(req.getPage())
        .pageSize(req.getPageSize())
        .search(req.getSearch())
        .build();

    return service.getTrashedRoles(domainReq)
        .map(resp -> ApiResponsePaginationRoleDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(resp.getData().stream().map(ProtoConverter::fromRoleResponseDeleteAt).toList())
            .setPaginationMeta(buildPaginationMeta(req.getPage(), req.getPageSize(), resp.getTotalRecords()))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsesRole> findByUserId(FindByIdUserRoleRequest req) {
    return service.getRolesByUserId(req.getUserId())
        .map(data -> ApiResponsesRole.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .addAllData(data.stream().map(ProtoConverter::fromRoleResponse).toList())
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}