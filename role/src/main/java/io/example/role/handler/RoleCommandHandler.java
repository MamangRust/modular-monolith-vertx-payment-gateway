package io.example.role.handler;

import com.google.protobuf.Empty;
import io.example.role.service.RoleCommandService;
import io.vertx.core.Future;
import pb.role.Role.*;
import pb.role.RoleCommand.*;

public class RoleCommandHandler implements pb.role.VertxRoleCommandServiceGrpcServer.RoleCommandServiceApi {
  private final RoleCommandService service;

  public RoleCommandHandler(RoleCommandService service) {
    this.service = service;
  }

  @Override
  public Future<ApiResponseRole> createRole(CreateRoleRequest req) {
    return service.createRole(req)
        .map(resp -> {
          var builder = ApiResponseRole.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromRoleResponse(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseRole> updateRole(UpdateRoleRequest req) {
    return service.updateRole(req)
        .map(resp -> {
          var builder = ApiResponseRole.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromRoleResponse(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseRoleDeleteAt> trashedRole(FindByIdRoleRequest req) {
    return service.trashRole(req.getRoleId())
        .map(resp -> {
          var builder = ApiResponseRoleDeleteAt.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromRoleResponseDeleteAt(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseRoleDeleteAt> restoreRole(FindByIdRoleRequest req) {
    return service.restoreRole(req.getRoleId())
        .map(resp -> {
          var builder = ApiResponseRoleDeleteAt.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromRoleResponseDeleteAt(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseRoleDelete> deleteRolePermanent(FindByIdRoleRequest req) {
    return service.deletePermanent(req.getRoleId())
        .map(resp -> ApiResponseRoleDelete.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .build());
  }

  @Override
  public Future<ApiResponseRoleAll> restoreAllRole(Empty req) {
    return service.restoreAllRoles()
        .map(resp -> ApiResponseRoleAll.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .build());
  }

  @Override
  public Future<ApiResponseRoleAll> deleteAllRolePermanent(Empty req) {
    return service.deleteAllPermanentRoles()
        .map(resp -> ApiResponseRoleAll.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .build());
  }
}
