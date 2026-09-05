package io.example.common.grpc;

import grpc.health.v1.HealthCheckRequest;
import grpc.health.v1.HealthCheckResponse;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.grpc.server.GrpcServer;
import java.util.concurrent.atomic.AtomicBoolean;

/** Standard grpc.health.v1 implementation for a Vert.x domain server. */
public final class GrpcHealthService implements grpc.health.v1.VertxHealthGrpcServer.HealthApi {
  private final String serviceName;
  private final AtomicBoolean serving = new AtomicBoolean(false);

  public GrpcHealthService() {
    this("");
  }

  public GrpcHealthService(String serviceName) {
    this.serviceName = serviceName == null ? "" : serviceName;
  }

  public GrpcHealthService bind(GrpcServer server) {
    bindAll(server);
    return this;
  }

  /** Bind health and start the listener; readiness is controlled by the caller. */
  public Future<Void> listen(Vertx vertx, GrpcServer server,
      Handler<HttpServerRequest> requestHandler, int port) {
    bind(server);
    return vertx.createHttpServer()
        .requestHandler(requestHandler)
        .listen(port)
        .mapEmpty();
  }

  public void setServing(boolean value) {
    serving.set(value);
  }

  public boolean isServing() {
    return serving.get();
  }

  @Override
  public Future<HealthCheckResponse> check(HealthCheckRequest request) {
    String requestedService = request == null ? "" : request.getService();
    HealthCheckResponse.ServingStatus status;
    if (!requestedService.isEmpty() && !requestedService.equals(serviceName)) {
      status = HealthCheckResponse.ServingStatus.SERVICE_UNKNOWN;
    } else {
      status = serving.get()
          ? HealthCheckResponse.ServingStatus.SERVING
          : HealthCheckResponse.ServingStatus.NOT_SERVING;
    }
    return Future.succeededFuture(HealthCheckResponse.newBuilder().setStatus(status).build());
  }
}
