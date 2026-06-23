package io.example.role.handler;

import com.google.protobuf.Empty;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.role.service.RoleCommandService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.role.Role.ApiResponseRole;
import pb.role.Role.ApiResponseRoleDeleteAt;
import pb.role.Role.FindByIdRoleRequest;
import pb.role.RoleCommand.ApiResponseRoleAll;
import pb.role.RoleCommand.ApiResponseRoleDelete;

@RequiredArgsConstructor
public class RoleCommandHandler implements pb.role.VertxRoleCommandServiceGrpcServer.RoleCommandServiceApi {
  private final RoleCommandService service;

  @Override
  public Future<ApiResponseRole> createRole(pb.role.RoleCommand.CreateRoleRequest req) {
    io.example.role.domain.requests.CreateRoleRequest domainReq = io.example.role.domain.requests.CreateRoleRequest.builder()
        .name(req.getName())
        .build();

    return service.createRole(domainReq)
        .map(data -> ApiResponseRole.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromRoleResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseRole> updateRole(pb.role.RoleCommand.UpdateRoleRequest req) {
    io.example.role.domain.requests.UpdateRoleRequest domainReq = io.example.role.domain.requests.UpdateRoleRequest.builder()
        .roleId(req.getId())
        .name(req.getName())
        .build();

    return service.updateRole(domainReq)
        .map(data -> ApiResponseRole.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromRoleResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseRoleDeleteAt> trashedRole(FindByIdRoleRequest req) {
    return service.trashRole(req.getRoleId())
        .map(data -> ApiResponseRoleDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromRoleResponseDeleteAt(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseRoleDeleteAt> restoreRole(FindByIdRoleRequest req) {
    return service.restoreRole(req.getRoleId())
        .map(data -> ApiResponseRoleDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromRoleResponseDeleteAt(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseRoleDelete> deleteRolePermanent(FindByIdRoleRequest req) {
    return service.deletePermanent(req.getRoleId())
        .map(v -> ApiResponseRoleDelete.newBuilder()
            .setStatus("success")
            .setMessage("Role deleted permanently")
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseRoleAll> restoreAllRole(Empty req) {
    return service.restoreAllRoles()
        .map(v -> ApiResponseRoleAll.newBuilder()
            .setStatus("success")
            .setMessage("All roles restored successfully")
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseRoleAll> deleteAllRolePermanent(Empty req) {
    return service.deleteAllPermanentRoles()
        .map(v -> ApiResponseRoleAll.newBuilder()
            .setStatus("success")
            .setMessage("All roles permanently deleted")
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}