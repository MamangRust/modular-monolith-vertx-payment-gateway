package io.example.user.handler;

import io.example.common.domain.PagedResult;
import io.example.user.model.UserResponse;
import io.example.user.model.UserResponseDeleteAt;
import io.example.user.service.UserQueryService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class UserQueryHandlerTest {

  @Mock
  private UserQueryService service;

  private UserQueryHandler handler;

  @BeforeEach
  void setUp() {
    handler = new UserQueryHandler(service);
  }

  private static UserResponse aResp(int id, String firstname) {
    return UserResponse.builder()
        .userId(id)
        .firstname(firstname)
        .lastname("Doe")
        .email(firstname.toLowerCase() + "@example.com")
        .createdAt("2026-06-26T10:00:00Z")
        .updatedAt("2026-06-26T10:00:00Z")
        .build();
  }

  private static UserResponseDeleteAt aRespDeleteAt(int id, String firstname) {
    return UserResponseDeleteAt.builder()
        .userId(id)
        .firstname(firstname)
        .lastname("Doe")
        .email(firstname.toLowerCase() + "@example.com")
        .createdAt("2026-06-26T10:00:00Z")
        .updatedAt("2026-06-26T10:00:00Z")
        .deletedAt("2026-06-25T10:00:00Z")
        .build();
  }

  /* ─── findAll ─── */

  @Test
  @DisplayName("findAll returns paginated response")
  void findAll(VertxTestContext ctx) {
    var data = List.of(aResp(1, "Alice"), aResp(2, "Bob"));
    when(service.getUsers(any())).thenReturn(Future.succeededFuture(new PagedResult<>(data, 2)));

    var req = pb.user.User.FindAllUserRequest.newBuilder().setPage(1).setPageSize(10).build();

    handler.findAll(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getMessage()).isEqualTo("OK");
          assertThat(resp.getDataCount()).isEqualTo(2);
          assertThat(resp.getData(0).getFirstname()).isEqualTo("Alice");
          assertThat(resp.getData(1).getFirstname()).isEqualTo("Bob");
          assertThat(resp.getPaginationMeta().getTotalRecords()).isEqualTo(2);
          assertThat(resp.getPaginationMeta().getCurrentPage()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── findById ─── */

  @Test
  @DisplayName("findById returns user response")
  void findById(VertxTestContext ctx) {
    when(service.getUserById(1)).thenReturn(Future.succeededFuture(aResp(1, "Charlie")));

    var req = pb.user.User.FindByIdUserRequest.newBuilder().setId(1).build();

    handler.findById(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getId()).isEqualTo(1);
          assertThat(resp.getData().getFirstname()).isEqualTo("Charlie");
          ctx.completeNow();
        })));
  }

  /* ─── findByActive ─── */

  @Test
  @DisplayName("findByActive returns paginated active users")
  void findByActive(VertxTestContext ctx) {
    var data = List.of(aRespDeleteAt(1, "Active User"));
    when(service.getActiveUsers(any())).thenReturn(Future.succeededFuture(new PagedResult<>(data, 1)));

    var req = pb.user.User.FindAllUserRequest.newBuilder().setPage(1).setPageSize(10).build();

    handler.findByActive(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          assertThat(resp.getData(0).getFirstname()).isEqualTo("Active User");
          assertThat(resp.getPaginationMeta().getTotalRecords()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── findByTrashed ─── */

  @Test
  @DisplayName("findByTrashed returns paginated trashed users")
  void findByTrashed(VertxTestContext ctx) {
    var data = List.of(aRespDeleteAt(1, "Trashed User"));
    when(service.getTrashedUsers(any())).thenReturn(Future.succeededFuture(new PagedResult<>(data, 1)));

    var req = pb.user.User.FindAllUserRequest.newBuilder().setPage(1).setPageSize(10).build();

    handler.findByTrashed(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          assertThat(resp.getData(0).getFirstname()).isEqualTo("Trashed User");
          assertThat(resp.getPaginationMeta().getTotalRecords()).isEqualTo(1);
          ctx.completeNow();
        })));
  }
}
