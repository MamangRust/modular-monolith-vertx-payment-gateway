package io.example.common.utils;

import io.grpc.Status;

/**
 * Maps internal decline reasons to ISO 8583 decline codes and HTTP-equivalent statuses.
 * These codes align with standard card network (Visa/Mastercard) decline reasons.
 */
public final class DeclineCodeMapper {

  private DeclineCodeMapper() {
  }

  public enum DeclineReason {
    INSUFFICIENT_FUNDS,
    CARD_INACTIVE,
    CARD_BLOCKED,
    CARD_LOST,
    CARD_STOLEN,
    CARD_EXPIRED,
    CANCELLED,
    VELOCITY_EXCEEDED,
    DUPLICATE_TRANSACTION,
    INVALID_MERCHANT,
    SUSPICIOUS_FRAUD,
    AMOUNT_LIMIT_EXCEEDED,
    DO_NOT_HONOR
  }

  /**
   * Maps a DeclineReason to a 2-character ISO 8583 decline code.
   */
  public static String toDeclineCode(DeclineReason reason) {
    return switch (reason) {
      case INSUFFICIENT_FUNDS -> "51";
      case CARD_INACTIVE -> "62";
      case CARD_BLOCKED -> "62";
      case CARD_LOST -> "41";
      case CARD_STOLEN -> "43";
      case CARD_EXPIRED -> "54";
      case CANCELLED -> "57";
      case VELOCITY_EXCEEDED -> "61";
      case DUPLICATE_TRANSACTION -> "94";
      case INVALID_MERCHANT -> "03";
      case SUSPICIOUS_FRAUD -> "59";
      case AMOUNT_LIMIT_EXCEEDED -> "61";
      case DO_NOT_HONOR -> "05";
    };
  }

  /**
   * Maps a DeclineReason to a human-readable description.
   */
  public static String toDescription(DeclineReason reason) {
    return switch (reason) {
      case INSUFFICIENT_FUNDS -> "Insufficient funds / available credit";
      case CARD_INACTIVE -> "Card is inactive";
      case CARD_BLOCKED -> "Card is blocked";
      case CARD_LOST -> "Lost card";
      case CARD_STOLEN -> "Stolen card";
      case CARD_EXPIRED -> "Expired card";
      case CANCELLED -> "Transaction cancelled";
      case VELOCITY_EXCEEDED -> "Exceeds withdrawal frequency limit";
      case DUPLICATE_TRANSACTION -> "Duplicate transaction";
      case INVALID_MERCHANT -> "Invalid merchant";
      case SUSPICIOUS_FRAUD -> "Suspected fraud";
      case AMOUNT_LIMIT_EXCEEDED -> "Exceeds amount limit";
      case DO_NOT_HONOR -> "Do not honor";
    };
  }

  /**
   * Maps a DeclineReason to an HTTP/gRPC status code for API responses.
   */
  public static Status toGrpcStatus(DeclineReason reason) {
    return switch (reason) {
      case INSUFFICIENT_FUNDS, AMOUNT_LIMIT_EXCEEDED -> Status.FAILED_PRECONDITION;
      case CARD_INACTIVE, CARD_BLOCKED, CARD_LOST, CARD_STOLEN, CARD_EXPIRED, CANCELLED ->
          Status.PERMISSION_DENIED;
      case DUPLICATE_TRANSACTION -> Status.ALREADY_EXISTS;
      case VELOCITY_EXCEEDED -> Status.RESOURCE_EXHAUSTED;
      case INVALID_MERCHANT -> Status.INVALID_ARGUMENT;
      case SUSPICIOUS_FRAUD, DO_NOT_HONOR -> Status.UNAVAILABLE;
    };
  }
}
