package io.example.common.exception.grpc;

public class UnauthorizedException extends GrpcException {
    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }

    @Override
    public io.grpc.Status.Code getGrpcStatusCode() {
        return io.grpc.Status.Code.UNAUTHENTICATED;
    }
}
