package io.example.common.utils;

import java.util.Random;

public final class CardNumberGenerator {

  private static final Random RANDOM = new Random();
  private static final String[] MASTERCARD_IINS = {"51", "52", "53", "54", "55"};

  private CardNumberGenerator() {
  }

  /**
   * Generate a Luhn-valid 16-digit Visa card number (IIN prefix 4).
   */
  public static String randomVisaCardNumber() {
    return generateWithPrefix("4");
  }

  /**
   * Generate a Luhn-valid 16-digit Mastercard card number (IIN prefix 51-55).
   */
  public static String randomMastercardNumber() {
    String prefix = MASTERCARD_IINS[RANDOM.nextInt(MASTERCARD_IINS.length)];
    return generateWithPrefix(prefix);
  }

  /**
   * Generate a Luhn-valid 16-digit card number with a given IIN prefix.
   */
  public static String generateWithPrefix(String prefix) {
    int totalLen = 16;
    int[] digits = new int[totalLen];

    // Set prefix digits
    for (int i = 0; i < prefix.length(); i++) {
      digits[i] = prefix.charAt(i) - '0';
    }

    // Fill remaining digits (except check digit) randomly
    for (int i = prefix.length(); i < totalLen - 1; i++) {
      digits[i] = RANDOM.nextInt(10);
    }

    // Compute Luhn check digit
    int sum = 0;
    for (int i = 0; i < totalLen - 1; i++) {
      int d = digits[i];
      if (i % 2 == 0) {
        d *= 2;
        if (d > 9) {
          d -= 9;
        }
      }
      sum += d;
    }
    digits[totalLen - 1] = (10 - (sum % 10)) % 10;

    StringBuilder sb = new StringBuilder(totalLen);
    for (int d : digits) {
      sb.append(d);
    }
    return sb.toString();
  }
}
