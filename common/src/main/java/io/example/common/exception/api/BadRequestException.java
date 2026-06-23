package io.example.common.exception.api;

public class BadRequestException extends ApiException {
  public BadRequestException(String message) {
    super(message, 400);
  }
}
