package io.example.common.exception.grpc;

public class ConflictException extends GrpcException {
    public ConflictException(String message) {
        super("CONFLICT", message);
    }

    @Override
    public io.grpc.Status.Code getGrpcStatusCode() {
        return io.grpc.Status.Code.ALREADY_EXISTS;
    }
}
