package io.example.common.exception;

public class ForbiddenException extends ApiException {
  public ForbiddenException(String message) {
    super(message, 403);
  }
}
