package io.example.common.exception.api;

public class NotFoundException extends ApiException {
  public NotFoundException(String message) {
    super(message, 404);
  }
}
