package io.example.apigateway.utils;

import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.vertx.core.json.JsonObject;

public final class ProtoMapper {
  private static final JsonFormat.Printer PRINTER = JsonFormat.printer()
      .includingDefaultValueFields()
      .preservingProtoFieldNames();

  private ProtoMapper() {}

  public static JsonObject toJson(MessageOrBuilder proto) {
    try {
      if (proto == null) return new JsonObject();
      return new JsonObject(PRINTER.print(proto));
    } catch (Exception e) {
      return new JsonObject().put("error", "Failed to serialize protobuf: " + e.getMessage());
    }
  }
}
