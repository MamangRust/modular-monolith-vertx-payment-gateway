package io.example.common.exception.grpc;

public abstract class GrpcException extends RuntimeException {
    private final String errorCode;

    protected GrpcException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected GrpcException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public abstract io.grpc.Status.Code getGrpcStatusCode();
}
