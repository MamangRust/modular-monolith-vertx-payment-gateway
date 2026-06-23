package io.example.common.exception.grpc;

public class NotFoundException extends GrpcException {
    public NotFoundException(String message) {
        super("NOT_FOUND", message);
    }

    @Override
    public io.grpc.Status.Code getGrpcStatusCode() {
        return io.grpc.Status.Code.NOT_FOUND;
    }
}
