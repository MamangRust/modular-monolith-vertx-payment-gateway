package io.example.common.exception.grpc;

public class InternalServerErrorException extends GrpcException {
    public InternalServerErrorException(String message, Throwable cause) {
        super("INTERNAL_ERROR", message, cause);
    }

    @Override
    public io.grpc.Status.Code getGrpcStatusCode() {
        return io.grpc.Status.Code.INTERNAL;
    }
}
