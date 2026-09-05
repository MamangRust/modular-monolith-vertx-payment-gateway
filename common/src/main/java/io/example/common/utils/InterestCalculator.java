package io.example.common.utils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Daily balance method interest calculation utility for credit card billing.
 *
 * <p>Formula: Interest = ∑(daily balance × DPR) over the billing cycle.
 * DPR (Daily Periodic Rate) = annual_rate_bps / 36500.
 * Amounts are in the smallest currency unit (e.g., IDR paise / satang).
 */
public final class InterestCalculator {

  private InterestCalculator() {
  }

  /**
   * Calculate interest using the daily balance method across a full cycle.
   *
   * @param annualRateBps annual interest rate in basis points (e.g. 1800 = 18.00%)
   * @param openingBalance opening balance at cycle start (paise/satang)
   * @param purchases      total purchase amount during cycle
   * @param payments       total payment amount during cycle
   * @param cycleDays      number of days in the billing cycle
   * @return interest amount in smallest currency unit
   */
  public static long calculateDailyBalanceInterest(
      int annualRateBps,
      long openingBalance,
      long purchases,
      long payments,
      int cycleDays) {

    if (cycleDays <= 0 || annualRateBps <= 0) {
      return 0;
    }

    double dpr = annualRateBps / 100.0 / 36500.0; // daily periodic rate from bps

    // Simplified daily balance: average balance weighted across cycle
    // Assuming purchases are spread evenly and payments are credited mid-cycle
    long avgDailyBalance = openingBalance + (purchases / 2) - (payments / 2);
    if (avgDailyBalance <= 0) {
      return 0; // No interest if balance is paid in full
    }

    double interest = avgDailyBalance * dpr * cycleDays;
    return Math.max(0, Math.round(interest));
  }

  /**
   * Calculate interest for a given balance and number of days.
   *
   * @param annualRateBps annual rate in bps
   * @param balance       current balance
   * @param days          number of days
   * @return interest amount
   */
  public static long calculateInterestForDays(int annualRateBps, long balance, int days) {
    if (days <= 0 || annualRateBps <= 0 || balance <= 0) {
      return 0;
    }
    double dpr = annualRateBps / 100.0 / 36500.0;
    return Math.round(balance * dpr * days);
  }

  /**
   * Calculate the number of days between two dates.
   */
  public static int daysBetween(LocalDate from, LocalDate to) {
    return (int) ChronoUnit.DAYS.between(from, to);
  }

  /**
   * Calculate minimum payment: max(10% of closing balance, minimumFloor).
   * Floor is 50,000 IDR paise (500 IDR).
   */
  public static long calculateMinimumPayment(long closingBalance, long minimumFloor) {
    if (closingBalance <= 0) {
      return 0;
    }
    long tenPercent = Math.round(closingBalance * 0.10);
    return Math.max(tenPercent, minimumFloor);
  }
}
