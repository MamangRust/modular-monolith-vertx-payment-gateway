package io.example.saldo.handler;

import com.google.protobuf.StringValue;

import io.example.saldo.model.Saldo;
import io.example.saldo.model.SaldoStats;
import pb.saldo.Saldo.SaldoResponse;
import pb.saldo.Saldo.SaldoResponseDeleteAt;
import pb.saldo.stats.SaldoStatsBalance.SaldoMonthBalanceResponse;
import pb.saldo.stats.SaldoStatsBalance.SaldoYearBalanceResponse;
import pb.saldo.stats.SaldoStatsTotal.SaldoMonthTotalBalanceResponse;
import pb.saldo.stats.SaldoStatsTotal.SaldoYearTotalBalanceResponse;

public class ProtoConverter {

  public static SaldoResponse toSaldoResponse(Saldo s) {
    if (s == null)
      return SaldoResponse.getDefaultInstance();
    return SaldoResponse.newBuilder()
        .setSaldoId(s.getId() != null ? s.getId() : 0)
        .setCardNumber(s.getCardNumber() != null ? s.getCardNumber() : "")
        .setTotalBalance(s.getTotalBalance() != null ? s.getTotalBalance().intValue() : 0)
        .setWithdrawAmount(s.getWithdrawAmount() != null ? s.getWithdrawAmount().intValue() : 0)
        .setWithdrawTime(s.getWithdrawTime() != null ? s.getWithdrawTime().toString() : "")
        .setCreatedAt(s.getCreatedAt() != null ? s.getCreatedAt().toString() : "")
        .setUpdatedAt(s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : "")
        .build();
  }

  public static SaldoResponseDeleteAt toSaldoDeleteAt(Saldo s) {
    if (s == null)
      return SaldoResponseDeleteAt.getDefaultInstance();
    var b = SaldoResponseDeleteAt.newBuilder()
        .setSaldoId(s.getId() != null ? s.getId() : 0)
        .setCardNumber(s.getCardNumber() != null ? s.getCardNumber() : "")
        .setTotalBalance(s.getTotalBalance() != null ? s.getTotalBalance().intValue() : 0)
        .setWithdrawAmount(s.getWithdrawAmount() != null ? s.getWithdrawAmount().intValue() : 0)
        .setWithdrawTime(s.getWithdrawTime() != null ? s.getWithdrawTime().toString() : "")
        .setCreatedAt(s.getCreatedAt() != null ? s.getCreatedAt().toString() : "")
        .setUpdatedAt(s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : "");

    if (s.getDeletedAt() != null) {
      b.setDeletedAt(StringValue.of(s.getDeletedAt().toString()));
    }
    return b.build();
  }

  public static SaldoResponse fromSaldoResponse(io.example.saldo.model.SaldoResponse s) {
    if (s == null)
      return SaldoResponse.getDefaultInstance();
    return SaldoResponse.newBuilder()
        .setSaldoId(s.getId() != null ? s.getId() : 0)
        .setCardNumber(s.getCardNumber() != null ? s.getCardNumber() : "")
        .setTotalBalance(s.getTotalBalance() != null ? s.getTotalBalance().intValue() : 0)
        .setCreatedAt(s.getCreatedAt() != null ? s.getCreatedAt() : "")
        .setUpdatedAt(s.getUpdatedAt() != null ? s.getUpdatedAt() : "")
        .build();
  }

  public static SaldoResponseDeleteAt fromSaldoResponseDeleteAt(io.example.saldo.model.SaldoResponseDeleteAt s) {
    if (s == null)
      return SaldoResponseDeleteAt.getDefaultInstance();
    var b = SaldoResponseDeleteAt.newBuilder()
        .setSaldoId(s.getId() != null ? s.getId() : 0)
        .setCardNumber(s.getCardNumber() != null ? s.getCardNumber() : "")
        .setTotalBalance(s.getTotalBalance() != null ? s.getTotalBalance().intValue() : 0)
        .setCreatedAt(s.getCreatedAt() != null ? s.getCreatedAt() : "")
        .setUpdatedAt(s.getUpdatedAt() != null ? s.getUpdatedAt() : "");

    if (s.getDeletedAt() != null) {
      b.setDeletedAt(StringValue.of(s.getDeletedAt()));
    }
    return b.build();
  }

  public static SaldoMonthTotalBalanceResponse toProtoMonthTotal(SaldoStats.MonthTotalBalance src) {
    if (src == null)
      return SaldoMonthTotalBalanceResponse.getDefaultInstance();
    return SaldoMonthTotalBalanceResponse.newBuilder()
        .setMonth(src.getMonth() != null ? src.getMonth() : "")
        .setYear(src.getYear() != null ? src.getYear() : "")
        .setTotalBalance(src.getTotalBalance() != null ? src.getTotalBalance().intValue() : 0)
        .build();
  }

  public static SaldoYearTotalBalanceResponse toProtoYearTotal(SaldoStats.YearTotalBalance src) {
    if (src == null)
      return SaldoYearTotalBalanceResponse.getDefaultInstance();
    return SaldoYearTotalBalanceResponse.newBuilder()
        .setYear(src.getYear() != null ? src.getYear() : "")
        .setTotalBalance(src.getTotalBalance() != null ? src.getTotalBalance().intValue() : 0)
        .build();
  }

  public static SaldoMonthBalanceResponse toProtoMonthBal(SaldoStats.MonthBalance src) {
    if (src == null)
      return SaldoMonthBalanceResponse.getDefaultInstance();
    return SaldoMonthBalanceResponse.newBuilder()
        .setMonth(src.getMonth() != null ? src.getMonth() : "")
        .setTotalBalance(src.getTotalBalance() != null ? src.getTotalBalance().intValue() : 0)
        .build();
  }

  public static SaldoYearBalanceResponse toProtoYearBal(SaldoStats.YearBalance src) {
    if (src == null)
      return SaldoYearBalanceResponse.getDefaultInstance();
    return SaldoYearBalanceResponse.newBuilder()
        .setYear(src.getYear() != null ? src.getYear() : "")
        .setTotalBalance(src.getTotalBalance() != null ? src.getTotalBalance().intValue() : 0)
        .build();
  }
}
