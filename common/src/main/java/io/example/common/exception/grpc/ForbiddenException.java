package io.example.common.exception.grpc;

public class ForbiddenException extends GrpcException {
    public ForbiddenException(String message) {
        super("FORBIDDEN", message);
    }

    @Override
    public io.grpc.Status.Code getGrpcStatusCode() {
        return io.grpc.Status.Code.PERMISSION_DENIED;
    }
}
