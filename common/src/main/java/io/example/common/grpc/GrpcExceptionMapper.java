package io.example.common.grpc;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.ConflictException;
import io.example.common.exception.grpc.GrpcException;
import io.example.common.exception.grpc.InsufficientBalanceException;
import io.example.common.exception.grpc.InternalServerErrorException;
import io.example.common.exception.grpc.NotFoundException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.vertx.core.Future;

public final class GrpcExceptionMapper {

    private GrpcExceptionMapper() {
    }

    /**
     * Maps any Throwable to a failed Future with proper gRPC Status.
     * This is the single place where domain exceptions → gRPC status mapping
     * happens.
     */
    public static <T> Future<T> toFailedFuture(Throwable throwable) {
        return Future.failedFuture(toStatusRuntimeException(throwable));
    }

    public static StatusRuntimeException toStatusRuntimeException(Throwable throwable) {
        if (throwable instanceof StatusRuntimeException sre) {
            return sre;
        }

        if (throwable instanceof GrpcException de) {
            return mapDomainException(de);
        }

        return Status.INTERNAL
                .withDescription("An unexpected error occurred")
                .withCause(throwable)
                .asRuntimeException();
    }

    private static StatusRuntimeException mapDomainException(GrpcException ex) {
        return switch (ex) {
            case NotFoundException nfe ->
                Status.NOT_FOUND
                        .withDescription(nfe.getMessage())
                        .asRuntimeException();

            case BadRequestException bre ->
                Status.INVALID_ARGUMENT
                        .withDescription(bre.getMessage())
                        .asRuntimeException();

            case InsufficientBalanceException ibe ->
                Status.FAILED_PRECONDITION
                        .withDescription(ibe.getMessage())
                        .asRuntimeException();

            case ConflictException ce ->
                Status.ALREADY_EXISTS
                        .withDescription(ce.getMessage())
                        .asRuntimeException();

            case InternalServerErrorException ie ->
                Status.INTERNAL
                        .withDescription(ie.getMessage())
                        .asRuntimeException();

            default ->
                Status.INTERNAL
                        .withDescription(ex.getMessage())
                        .asRuntimeException();
        };
    }
}