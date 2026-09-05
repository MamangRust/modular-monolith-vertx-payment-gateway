package io.example.common.exception.grpc;

public class FailedPreconditionException extends GrpcException {
  public FailedPreconditionException(String message) {
    super("FAILED_PRECONDITION", message);
  }

  @Override
  public io.grpc.Status.Code getGrpcStatusCode() {
    return io.grpc.Status.Code.FAILED_PRECONDITION;
  }
}
