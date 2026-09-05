package io.example.apigateway.utils;

import io.example.common.exception.api.BadRequestException;
import io.example.common.exception.api.ForbiddenException;
import io.example.common.exception.api.NotFoundException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrpcGatewayUtilsTest {

  /* ─── JSON extraction helpers ─── */

  @Test
  @DisplayName("getJsonString returns value when present")
  void getJsonStringPresent() {
    var json = new JsonObject().put("name", "Alice");
    assertThat(GrpcGatewayUtils.getJsonString(json, "name", "default")).isEqualTo("Alice");
  }

  @Test
  @DisplayName("getJsonString returns default when key missing")
  void getJsonStringMissing() {
    var json = new JsonObject();
    assertThat(GrpcGatewayUtils.getJsonString(json, "name", "default")).isEqualTo("default");
  }

  @Test
  @DisplayName("getJsonString returns default when value is null")
  void getJsonStringNullValue() {
    var json = new JsonObject().putNull("name");
    assertThat(GrpcGatewayUtils.getJsonString(json, "name", "default")).isEqualTo("default");
  }

  @Test
  @DisplayName("getJsonInteger with boxed return type returns value when present")
  void getJsonIntegerBoxedPresent() {
    var json = new JsonObject().put("count", 42);
    assertThat(GrpcGatewayUtils.getJsonInteger(json, "count", 0)).isEqualTo(42);
  }

  @Test
  @DisplayName("getJsonInteger with boxed return type returns default when key missing")
  void getJsonIntegerBoxedMissing() {
    var json = new JsonObject();
    assertThat(GrpcGatewayUtils.getJsonInteger(json, "count", 0)).isEqualTo(0);
  }

  @Test
  @DisplayName("getJsonInteger with primitive return type")
  void getJsonIntegerPrimitive() {
    var json = new JsonObject().put("count", 42);
    assertThat(GrpcGatewayUtils.getJsonInteger(json, "count", 0)).isEqualTo(42);
  }

  @Test
  @DisplayName("getJsonLong extracts long from integer value")
  void getJsonLongFromInt() {
    var json = new JsonObject().put("amount", 1000);
    assertThat(GrpcGatewayUtils.getJsonLong(json, "amount", 0L)).isEqualTo(1000L);
  }

  @Test
  @DisplayName("getJsonLong extracts long from long value")
  void getJsonLongFromLong() {
    var json = new JsonObject().put("amount", 9999999999L);
    assertThat(GrpcGatewayUtils.getJsonLong(json, "amount", 0L)).isEqualTo(9999999999L);
  }

  @Test
  @DisplayName("getJsonLong returns default when key missing")
  void getJsonLongMissing() {
    var json = new JsonObject();
    assertThat(GrpcGatewayUtils.getJsonLong(json, "amount", -1L)).isEqualTo(-1L);
  }

  /* ─── Error handling ─── */

  @Test
  @DisplayName("handleError maps NOT_FOUND to 404 via sendError")
  void handleErrorNotFound() {
    var err = new StatusRuntimeException(Status.NOT_FOUND.withDescription("user not found"));
    var ctx = mock(RoutingContext.class);
    var response = mock(io.vertx.core.http.HttpServerResponse.class);
    when(ctx.response()).thenReturn(response);
    when(response.setStatusCode(404)).thenReturn(response);
    when(response.putHeader("Content-Type", "application/json")).thenReturn(response);

    GrpcGatewayUtils.handleError(ctx, err);

    verify(response).setStatusCode(404);
  }

  @Test
  @DisplayName("handleError maps INVALID_ARGUMENT to 400 via sendError")
  void handleErrorInvalidArgument() {
    var err = new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("bad input"));
    var ctx = mock(RoutingContext.class);
    var response = mock(io.vertx.core.http.HttpServerResponse.class);
    when(ctx.response()).thenReturn(response);
    when(response.setStatusCode(400)).thenReturn(response);
    when(response.putHeader("Content-Type", "application/json")).thenReturn(response);

    GrpcGatewayUtils.handleError(ctx, err);

    verify(response).setStatusCode(400);
  }

  @Test
  @DisplayName("handleError maps UNAUTHENTICATED to 401 via sendError")
  void handleErrorUnauthenticated() {
    var err = new StatusRuntimeException(Status.UNAUTHENTICATED.withDescription("invalid token"));
    var ctx = mock(RoutingContext.class);
    var response = mock(io.vertx.core.http.HttpServerResponse.class);
    when(ctx.response()).thenReturn(response);
    when(response.setStatusCode(401)).thenReturn(response);
    when(response.putHeader("Content-Type", "application/json")).thenReturn(response);

    GrpcGatewayUtils.handleError(ctx, err);

    verify(response).setStatusCode(401);
  }

  @Test
  @DisplayName("handleError maps PERMISSION_DENIED to 403 via sendError")
  void handleErrorPermissionDenied() {
    var err = new StatusRuntimeException(Status.PERMISSION_DENIED.withDescription("no access"));
    var ctx = mock(RoutingContext.class);
    var response = mock(io.vertx.core.http.HttpServerResponse.class);
    when(ctx.response()).thenReturn(response);
    when(response.setStatusCode(403)).thenReturn(response);
    when(response.putHeader("Content-Type", "application/json")).thenReturn(response);

    GrpcGatewayUtils.handleError(ctx, err);

    verify(response).setStatusCode(403);
  }

  @Test
  @DisplayName("handleError maps UNAVAILABLE to 503 via sendError")
  void handleErrorUnavailable() {
    var err = new StatusRuntimeException(Status.UNAVAILABLE.withDescription("service down"));
    var ctx = mock(RoutingContext.class);
    var response = mock(io.vertx.core.http.HttpServerResponse.class);
    when(ctx.response()).thenReturn(response);
    when(response.setStatusCode(503)).thenReturn(response);
    when(response.putHeader("Content-Type", "application/json")).thenReturn(response);

    GrpcGatewayUtils.handleError(ctx, err);

    verify(response).setStatusCode(503);
  }

  @Test
  @DisplayName("handleError maps ALREADY_EXISTS to 409 via sendError")
  void handleErrorAlreadyExists() {
    var err = new StatusRuntimeException(Status.ALREADY_EXISTS.withDescription("duplicate"));
    var ctx = mock(RoutingContext.class);
    var response = mock(io.vertx.core.http.HttpServerResponse.class);
    when(ctx.response()).thenReturn(response);
    when(response.setStatusCode(409)).thenReturn(response);
    when(response.putHeader("Content-Type", "application/json")).thenReturn(response);

    GrpcGatewayUtils.handleError(ctx, err);

    verify(response).setStatusCode(409);
  }

  @Test
  @DisplayName("handleError maps ApiException to its own status code")
  void handleErrorApiException() {
    var err = new NotFoundException("user not found");
    var ctx = mock(RoutingContext.class);
    var response = mock(io.vertx.core.http.HttpServerResponse.class);
    when(ctx.response()).thenReturn(response);
    when(response.setStatusCode(404)).thenReturn(response);
    when(response.putHeader("Content-Type", "application/json")).thenReturn(response);

    GrpcGatewayUtils.handleError(ctx, err);

    verify(response).setStatusCode(404);
  }

  @Test
  @DisplayName("handleRouteFailure maps ApiException to its own status code")
  void handleRouteFailureApiException() {
    var ctx = mock(RoutingContext.class);
    var response = mock(io.vertx.core.http.HttpServerResponse.class);
    when(ctx.failure()).thenReturn(new BadRequestException("email is required"));
    when(ctx.response()).thenReturn(response);
    when(response.setStatusCode(400)).thenReturn(response);
    when(response.putHeader("Content-Type", "application/json")).thenReturn(response);

    GrpcGatewayUtils.handleRouteFailure(ctx);

    verify(response).setStatusCode(400);
  }

  @Test
  @DisplayName("handleRouteFailure maps IllegalArgumentException to 400")
  void handleRouteFailureIllegalArgument() {
    var ctx = mock(RoutingContext.class);
    var response = mock(io.vertx.core.http.HttpServerResponse.class);
    when(ctx.failure()).thenReturn(new IllegalArgumentException("invalid path parameter"));
    when(ctx.response()).thenReturn(response);
    when(response.setStatusCode(400)).thenReturn(response);
    when(response.putHeader("Content-Type", "application/json")).thenReturn(response);

    GrpcGatewayUtils.handleRouteFailure(ctx);

    verify(response).setStatusCode(400);
  }

  @Test
  @DisplayName("handleRouteFailure maps ForbiddenException to 403")
  void handleRouteFailureForbidden() {
    var ctx = mock(RoutingContext.class);
    var response = mock(io.vertx.core.http.HttpServerResponse.class);
    when(ctx.failure()).thenReturn(new ForbiddenException("no access"));
    when(ctx.response()).thenReturn(response);
    when(response.setStatusCode(403)).thenReturn(response);
    when(response.putHeader("Content-Type", "application/json")).thenReturn(response);

    GrpcGatewayUtils.handleRouteFailure(ctx);

    verify(response).setStatusCode(403);
  }

  @Test
  @DisplayName("handleRouteFailure falls back to 500 for unknown exceptions")
  void handleRouteFailureUnknown() {
    var ctx = mock(RoutingContext.class);
    var response = mock(io.vertx.core.http.HttpServerResponse.class);
    when(ctx.failure()).thenReturn(new RuntimeException("boom"));
    when(ctx.response()).thenReturn(response);
    when(response.setStatusCode(500)).thenReturn(response);
    when(response.putHeader("Content-Type", "application/json")).thenReturn(response);

    GrpcGatewayUtils.handleRouteFailure(ctx);

    verify(response).setStatusCode(500);
  }

  @Test
  @DisplayName("handleRouteFailure maps null failure with status 401 (JWT middleware) to 401")
  void handleRouteFailureNullFailureWithStatus() {
    var ctx = mock(RoutingContext.class);
    var response = mock(io.vertx.core.http.HttpServerResponse.class);
    when(ctx.failure()).thenReturn(null);
    when(ctx.statusCode()).thenReturn(401);
    when(ctx.response()).thenReturn(response);
    when(response.setStatusCode(401)).thenReturn(response);
    when(response.putHeader("Content-Type", "application/json")).thenReturn(response);

    GrpcGatewayUtils.handleRouteFailure(ctx);

    verify(response).setStatusCode(401);
  }

  @Test
  @DisplayName("handleRouteFailure skips when response already ended")
  void handleRouteFailureResponseEnded() {
    var ctx = mock(RoutingContext.class);
    var response = mock(io.vertx.core.http.HttpServerResponse.class);
    when(ctx.response()).thenReturn(response);
    when(response.ended()).thenReturn(true);

    GrpcGatewayUtils.handleRouteFailure(ctx);

    verify(response, never()).setStatusCode(anyInt());
  }

  @Test
  @DisplayName("handleError maps unknown gRPC status to 500 via sendError")
  void handleErrorUnknown() {
    var err = new StatusRuntimeException(Status.DATA_LOSS.withDescription("corrupted"));
    var ctx = mock(RoutingContext.class);
    var response = mock(io.vertx.core.http.HttpServerResponse.class);
    when(ctx.response()).thenReturn(response);
    when(response.setStatusCode(500)).thenReturn(response);
    when(response.putHeader("Content-Type", "application/json")).thenReturn(response);

    GrpcGatewayUtils.handleError(ctx, err);

    verify(response).setStatusCode(500);
  }

  @Test
  @DisplayName("handleError calls ctx.fail for non-gRPC exceptions")
  void handleErrorNonGrpc() {
    var err = new RuntimeException("unexpected");
    var ctx = mock(RoutingContext.class);

    GrpcGatewayUtils.handleError(ctx, err);

    verify(ctx).fail(500, err);
  }

  /* ─── Path parameter parsing ─── */

  @Test
  @DisplayName("getSafePathInt parses valid integer")
  void getSafePathIntValid(@Mock RoutingContext ctx) {
    when(ctx.pathParam("id")).thenReturn("123");
    assertThat(GrpcGatewayUtils.getSafePathInt(ctx, "id")).isEqualTo(123);
  }

  @Test
  @DisplayName("getSafePathInt throws on non-integer param")
  void getSafePathIntInvalid(@Mock RoutingContext ctx) {
    when(ctx.pathParam("id")).thenReturn("abc");
    assertThrows(IllegalArgumentException.class,
        () -> GrpcGatewayUtils.getSafePathInt(ctx, "id"));
  }

  /* ─── Query parameter extraction ─── */

  @Test
  @DisplayName("getQueryInt returns parsed value")
  void getQueryIntPresent(@Mock RoutingContext ctx) {
    when(ctx.queryParams()).thenReturn(
        MultiMap.caseInsensitiveMultiMap().add("page", "3"));
    assertThat(GrpcGatewayUtils.getQueryInt(ctx, "page", 1)).isEqualTo(3);
  }

  @Test
  @DisplayName("getQueryInt returns default when param missing")
  void getQueryIntMissing(@Mock RoutingContext ctx) {
    when(ctx.queryParams()).thenReturn(MultiMap.caseInsensitiveMultiMap());
    assertThat(GrpcGatewayUtils.getQueryInt(ctx, "page", 1)).isEqualTo(1);
  }

  @Test
  @DisplayName("getQueryInt returns default when param not a number")
  void getQueryIntInvalid(@Mock RoutingContext ctx) {
    when(ctx.queryParams()).thenReturn(
        MultiMap.caseInsensitiveMultiMap().add("page", "abc"));
    assertThat(GrpcGatewayUtils.getQueryInt(ctx, "page", 1)).isEqualTo(1);
  }

  @Test
  @DisplayName("getQueryString returns value when present")
  void getQueryStringPresent(@Mock RoutingContext ctx) {
    when(ctx.queryParams()).thenReturn(
        MultiMap.caseInsensitiveMultiMap().add("sort", "desc"));
    assertThat(GrpcGatewayUtils.getQueryString(ctx, "sort", "asc")).isEqualTo("desc");
  }

  @Test
  @DisplayName("getQueryString returns default when param missing")
  void getQueryStringMissing(@Mock RoutingContext ctx) {
    when(ctx.queryParams()).thenReturn(MultiMap.caseInsensitiveMultiMap());
    assertThat(GrpcGatewayUtils.getQueryString(ctx, "sort", "asc")).isEqualTo("asc");
  }
}
