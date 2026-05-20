package io.example.common.utils;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

  private DateUtils() {
  }

  public static String formatExpDate(com.google.protobuf.Timestamp protoTs) {
    if (protoTs == null || (protoTs.getSeconds() == 0 && protoTs.getNanos() == 0)) {
      return Instant.now().toString().split("T")[0];
    }
    return Instant.ofEpochSecond(protoTs.getSeconds(), protoTs.getNanos())
        .atZone(java.time.ZoneOffset.UTC)
        .format(DateTimeFormatter.ISO_LOCAL_DATE);
  }
}
