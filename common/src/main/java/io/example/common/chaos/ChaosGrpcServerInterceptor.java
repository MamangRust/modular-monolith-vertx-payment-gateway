package io.example.common.chaos;

import io.grpc.Status;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.grpc.server.GrpcServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps a {@link GrpcServer} as a {@link Handler Handler&lt;HttpServerRequest&gt;}
 * that intercepts incoming gRPC calls and injects latency or faults
 * according to chaos policies of type {@code "grpc"}.
 *
 * <p>Usage — replace the normal {@code .requestHandler(grpcServer)} with:
 * <pre>{@code
 * GrpcServer grpcServer = GrpcServer.server(vertx);
 * // ... bind handlers ...
 * Handler<HttpServerRequest> chaosHandler =
 *     new ChaosGrpcServerInterceptor(grpcServer, chaosManager, vertx);
 * vertx.createHttpServer()
 *     .requestHandler(chaosHandler)
 *     .listen(grpcPort);
 * }</pre>
 */
public class ChaosGrpcServerInterceptor implements Handler<HttpServerRequest> {
  private static final Logger log = LoggerFactory.getLogger(ChaosGrpcServerInterceptor.class);

  private static final String GRPC_CONTENT_TYPE = "application/grpc";

  private final GrpcServer delegate;
  private final ChaosManager manager;
  private final Vertx vertx;

  public ChaosGrpcServerInterceptor(GrpcServer delegate, ChaosManager manager, Vertx vertx) {
    this.delegate = delegate;
    this.manager = manager;
    this.vertx = vertx;
  }

  @Override
  public void handle(HttpServerRequest request) {
    String path = request.path();
    if (path == null || path.isEmpty()) {
      delegate.handle(request);
      return;
    }

    // gRPC method path is "/package.ServiceName/MethodName"
    // Strip leading "/" to use as the chaos target
    String target = path.startsWith("/") ? path.substring(1) : path;

    // Capture the matched policy in an effectively-final variable for lambda use
    ChaosPolicy matched = manager.evaluate("grpc", target);
    if (matched == null) {
      matched = manager.evaluate("grpc", path);
    }
    final ChaosPolicy policy = matched;

    if (policy != null && policy.isEnabled() && Math.random() < policy.getErrorChance()) {
      log.info("🔥 Injecting gRPC chaos [Policy: {}] to method: {}", policy.getName(), target);

      long latency = policy.getLatencyMs();
      if (latency > 0) {
        vertx.setTimer(latency, id -> {
          if (shouldReturnError(policy)) {
            writeGrpcError(request.response(), policy);
          } else {
            delegate.handle(request);
          }
        });
      } else if (shouldReturnError(policy)) {
        writeGrpcError(request.response(), policy);
      } else {
        delegate.handle(request);
      }
    } else {
      delegate.handle(request);
    }
  }

  private boolean shouldReturnError(ChaosPolicy policy) {
    return policy.getGrpcStatus() != null
        || policy.getErrorMessage() != null
        || policy.getErrorCode() != 0;
  }

  /**
   * Write a gRPC error response over HTTP/2.
   *
   * <p>gRPC always returns HTTP 200, with the actual status code and message
   * carried in the {@code grpc-status} and {@code grpc-message} trailers.
   * For immediate error responses we send them as headers before {@code end()}.
   */
  private void writeGrpcError(HttpServerResponse response, ChaosPolicy policy) {
    Status grpcStatus = resolveGrpcStatus(policy);
    String message = policy.getErrorMessage() != null
        ? policy.getErrorMessage()
        : "Simulated gRPC chaos fault";

    response
        .setStatusCode(200)
        .putHeader("content-type", GRPC_CONTENT_TYPE)
        .putHeader("grpc-status", String.valueOf(grpcStatus.getCode().value()))
        .putHeader("grpc-message", message)
        .end();
  }

  /**
   * Map the {@code grpcStatus} string from the policy to an {@link Status}.
   *
   * <p>Supports the common gRPC status names as defined in {@link io.grpc.Status}.
   * Defaults to {@link Status#UNAVAILABLE} when the string is not recognised.
   */
  static Status resolveGrpcStatus(ChaosPolicy policy) {
    String code = policy.getGrpcStatus();
    if (code == null || code.isBlank()) {
      return Status.UNAVAILABLE;
    }

    return switch (code.toUpperCase()) {
      case "OK" -> Status.OK;
      case "CANCELLED" -> Status.CANCELLED;
      case "UNKNOWN" -> Status.UNKNOWN;
      case "INVALID_ARGUMENT" -> Status.INVALID_ARGUMENT;
      case "DEADLINE_EXCEEDED" -> Status.DEADLINE_EXCEEDED;
      case "NOT_FOUND" -> Status.NOT_FOUND;
      case "ALREADY_EXISTS" -> Status.ALREADY_EXISTS;
      case "PERMISSION_DENIED" -> Status.PERMISSION_DENIED;
      case "UNAUTHENTICATED" -> Status.UNAUTHENTICATED;
      case "RESOURCE_EXHAUSTED" -> Status.RESOURCE_EXHAUSTED;
      case "FAILED_PRECONDITION" -> Status.FAILED_PRECONDITION;
      case "ABORTED" -> Status.ABORTED;
      case "OUT_OF_RANGE" -> Status.OUT_OF_RANGE;
      case "UNIMPLEMENTED" -> Status.UNIMPLEMENTED;
      case "INTERNAL" -> Status.INTERNAL;
      case "UNAVAILABLE" -> Status.UNAVAILABLE;
      case "DATA_LOSS" -> Status.DATA_LOSS;
      default -> {
        log.warn("Unknown grpcStatus '{}' in policy '{}', defaulting to UNAVAILABLE", code, policy.getName());
        yield Status.UNAVAILABLE;
      }
    };
  }
}
