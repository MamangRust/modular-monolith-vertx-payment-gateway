package io.example.common.utils;

import java.util.Random;

public final class CardNumberGenerator {

  private static final Random RANDOM = new Random();

  private CardNumberGenerator() {
  }

  public static String randomVisaCardNumber() {
    int[] digits = new int[16];

    digits[0] = 4;

    for (int i = 1; i < 15; i++) {
      digits[i] = RANDOM.nextInt(10);
    }

    int sum = 0;
    for (int i = 0; i < 15; i++) {
      int d = digits[i];
      if (i % 2 == 0) {
        d *= 2;
        if (d > 9) {
          d -= 9;
        }
      }
      sum += d;
    }
    digits[15] = (10 - (sum % 10)) % 10;

    StringBuilder sb = new StringBuilder(16);
    for (int d : digits) {
      sb.append(d);
    }

    return sb.toString();
  }
}
