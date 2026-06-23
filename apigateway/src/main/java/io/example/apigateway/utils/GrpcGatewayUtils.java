package io.example.apigateway.utils;

import com.google.protobuf.MessageOrBuilder;

import io.example.common.exception.api.BadRequestException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

public class GrpcGatewayUtils {

    public static void sendResponse(RoutingContext ctx, MessageOrBuilder proto, int httpStatus) {
        JsonObject json = ProtoMapper.toJson(proto);
        ctx.response()
                .setStatusCode(httpStatus)
                .putHeader("Content-Type", "application/json")
                .end(json.encode());
    }

    public static void sendSuccess(RoutingContext ctx, int httpStatus, String message) {
        ctx.response()
                .setStatusCode(httpStatus)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                        .put("status", "success")
                        .put("message", message)
                        .encode());
    }

    public static void sendError(RoutingContext ctx, int httpStatus, String message) {
        ctx.response()
                .setStatusCode(httpStatus)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                        .put("status", "error")
                        .put("message", message)
                        .encode());
    }

    public static void handleError(RoutingContext ctx, Throwable err) {
        if (err instanceof StatusRuntimeException sre) {
            Status.Code code = sre.getStatus().getCode();
            String description = sre.getStatus().getDescription();

            int httpStatus = switch (code) {
                case NOT_FOUND -> 404;
                case INVALID_ARGUMENT, FAILED_PRECONDITION, ALREADY_EXISTS -> 400;
                case UNAUTHENTICATED -> 401;
                case PERMISSION_DENIED -> 403;
                case UNAVAILABLE -> 503;
                default -> 500;
            };

            sendError(ctx, httpStatus, description != null ? description : code.name());
        } else {
            ctx.fail(500, err);
        }
    }

    public static int getSafePathInt(RoutingContext ctx, String param) {
        try {
            return Integer.parseInt(ctx.pathParam(param));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid path parameter: " + param + " must be an integer");
        }
    }

    public static String getJsonString(JsonObject json, String key, String defaultValue) {
        String value = json.getString(key);
        return value != null ? value : defaultValue;
    }

    public static Integer getJsonInteger(JsonObject json, String key, Integer defaultValue) {
        Integer value = json.getInteger(key);
        return value != null ? value : defaultValue;
    }

    public static int getJsonInteger(JsonObject json, String key, int defaultValue) {
        try {
            return json.getInteger(key, defaultValue);
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    public static int getQueryInt(RoutingContext ctx, String key, int defaultValue) {
        try {
            return ctx.queryParams().contains(key) ? Integer.parseInt(ctx.queryParams().get(key)) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static String getQueryString(RoutingContext ctx, String key, String defaultValue) {
        return ctx.queryParams().contains(key) ? ctx.queryParams().get(key) : defaultValue;
    }

    public static String getFormString(RoutingContext ctx, String key, String defaultValue) {
        String value = ctx.request().getFormAttribute(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    public static int getFormInteger(RoutingContext ctx, String key, int defaultValue) {
        String value = ctx.request().getFormAttribute(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static String getRequiredFormString(RoutingContext ctx, String key) {
        String value = ctx.request().getFormAttribute(key);
        if (value == null || value.isBlank()) {
            throw new BadRequestException(key + " is required");
        }
        return value;
    }

    public static int getRequiredFormInteger(RoutingContext ctx, String key) {
        String value = ctx.request().getFormAttribute(key);
        if (value == null || value.isBlank()) {
            throw new BadRequestException(key + " is required");
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new BadRequestException(key + " must be a valid integer");
        }
    }

    public static FileUpload getFileUpload(RoutingContext ctx, String fieldName) {
        return ctx.fileUploads().stream()
                .filter(f -> fieldName.equals(f.name()))
                .findFirst()
                .orElse(null);
    }
}