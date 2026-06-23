package io.example.common.exception.grpc;

public class BadRequestException extends GrpcException {
    public BadRequestException(String message) {
        super("BAD_REQUEST", message);
    }

    @Override
    public io.grpc.Status.Code getGrpcStatusCode() {
        return io.grpc.Status.Code.INVALID_ARGUMENT;
    }
}
