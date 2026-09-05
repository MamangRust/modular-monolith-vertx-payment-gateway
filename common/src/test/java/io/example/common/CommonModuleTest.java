package io.example.common;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.ConflictException;
import io.example.common.exception.grpc.InsufficientBalanceException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.exception.grpc.UnauthorizedException;
import io.example.common.grpc.GrpcExceptionMapper;
import io.example.common.utils.CardNumberGenerator;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommonModuleTest {

  /* ───────── CardNumberGenerator tests ───────── */

  @Test
  @DisplayName("randomVisaCardNumber generates a 16-digit number starting with 4")
  void randomVisaCardNumberStartsWith4() {
    String number = CardNumberGenerator.randomVisaCardNumber();
    assertThat(number).hasSize(16);
    assertThat(number).startsWith("4");
  }

  @Test
  @DisplayName("randomVisaCardNumber is Luhn-valid")
  void randomVisaCardNumberIsLuhnValid() {
    String number = CardNumberGenerator.randomVisaCardNumber();
    assertThat(luhnCheck(number)).isTrue();
  }

  @Test
  @DisplayName("randomMastercardNumber generates a 16-digit number starting with 51-55")
  void randomMastercardNumberStartsWithValidIin() {
    String number = CardNumberGenerator.randomMastercardNumber();
    assertThat(number).hasSize(16);
    String prefix = number.substring(0, 2);
    assertThat(prefix).isIn("51", "52", "53", "54", "55");
  }

  @Test
  @DisplayName("randomMastercardNumber is Luhn-valid")
  void randomMastercardNumberIsLuhnValid() {
    String number = CardNumberGenerator.randomMastercardNumber();
    assertThat(luhnCheck(number)).isTrue();
  }

  @Test
  @DisplayName("generateWithPrefix produces a Luhn-valid number of correct length")
  void generateWithPrefixIsLuhnValid() {
    String number = CardNumberGenerator.generateWithPrefix("6011");
    assertThat(number).hasSize(16);
    assertThat(number).startsWith("6011");
    assertThat(luhnCheck(number)).isTrue();
  }

  /* ───────── GrpcExceptionMapper tests ───────── */

  @Test
  @DisplayName("GrpcExceptionMapper passes through StatusRuntimeException unchanged")
  void passesThroughStatusRuntimeException() {
    var original = Status.UNAUTHENTICATED.withDescription("bad token").asRuntimeException();
    var mapped = GrpcExceptionMapper.toStatusRuntimeException(original);
    assertThat(mapped).isSameAs(original);
  }

  @Test
  @DisplayName("GrpcExceptionMapper maps unknown exceptions to INTERNAL")
  void mapsUnknownExceptionToInternal() {
    var mapped = GrpcExceptionMapper.toStatusRuntimeException(new RuntimeException("boom"));
    assertThat(mapped.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
  }

  @Test
  @DisplayName("GrpcExceptionMapper maps NotFoundException to NOT_FOUND")
  void mapsNotFoundExceptionToNotFound() {
    var mapped = GrpcExceptionMapper.toStatusRuntimeException(new NotFoundException("missing"));
    assertThat(mapped.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
  }

  @Test
  @DisplayName("GrpcExceptionMapper maps BadRequestException to INVALID_ARGUMENT")
  void mapsBadRequestExceptionToInvalidArgument() {
    var mapped = GrpcExceptionMapper.toStatusRuntimeException(new BadRequestException("bad"));
    assertThat(mapped.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
  }

  @Test
  @DisplayName("GrpcExceptionMapper maps ConflictException to ALREADY_EXISTS")
  void mapsConflictExceptionToAlreadyExists() {
    var mapped = GrpcExceptionMapper.toStatusRuntimeException(new ConflictException("duplicate"));
    assertThat(mapped.getStatus().getCode()).isEqualTo(Status.Code.ALREADY_EXISTS);
  }

  @Test
  @DisplayName("GrpcExceptionMapper maps InsufficientBalanceException to FAILED_PRECONDITION")
  void mapsInsufficientBalanceExceptionToFailedPrecondition() {
    var mapped = GrpcExceptionMapper.toStatusRuntimeException(new InsufficientBalanceException(10, 100));
    assertThat(mapped.getStatus().getCode()).isEqualTo(Status.Code.FAILED_PRECONDITION);
  }

  @Test
  @DisplayName("GrpcExceptionMapper maps UnauthorizedException to UNAUTHENTICATED (401)")
  void mapsUnauthorizedExceptionToUnauthenticated() {
    var mapped = GrpcExceptionMapper.toStatusRuntimeException(new UnauthorizedException("Invalid credentials"));
    assertThat(mapped.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
  }

  /* ───────── Luhn helper ───────── */

  private static boolean luhnCheck(String cardNumber) {
    int sum = 0;
    boolean alternate = false;
    for (int i = cardNumber.length() - 1; i >= 0; i--) {
      int n = cardNumber.charAt(i) - '0';
      if (alternate) {
        n *= 2;
        if (n > 9) n -= 9;
      }
      sum += n;
      alternate = !alternate;
    }
    return (sum % 10) == 0;
  }
}
