package io.example.common.grpc;

import grpc.health.v1.HealthCheckRequest;
import grpc.health.v1.HealthCheckResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GrpcHealthServiceTest {
  @Test
  void startsNotServingAndBecomesServing() {
    GrpcHealthService service = new GrpcHealthService("transaction");
    HealthCheckRequest request = HealthCheckRequest.newBuilder().setService("transaction").build();

    assertThat(service.check(request).toCompletionStage().toCompletableFuture().join().getStatus())
        .isEqualTo(HealthCheckResponse.ServingStatus.NOT_SERVING);

    service.setServing(true);

    assertThat(service.check(request).toCompletionStage().toCompletableFuture().join().getStatus())
        .isEqualTo(HealthCheckResponse.ServingStatus.SERVING);
  }

  @Test
  void unknownServiceIsReportedExplicitly() {
    GrpcHealthService service = new GrpcHealthService("auth");
    HealthCheckRequest request = HealthCheckRequest.newBuilder().setService("card").build();

    assertThat(service.check(request).toCompletionStage().toCompletableFuture().join().getStatus())
        .isEqualTo(HealthCheckResponse.ServingStatus.SERVICE_UNKNOWN);
  }
}
