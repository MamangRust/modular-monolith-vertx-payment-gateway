package io.example.withdraw.handler;

import io.example.withdraw.model.Withdraw;
import io.example.withdraw.model.WithdrawResponse;
import io.example.withdraw.model.WithdrawResponseDeleteAt;

public class ProtoConverter {

  public static pb.withdraw.Withdraw.WithdrawResponse toResponse(Withdraw model) {
    if (model == null)
      return pb.withdraw.Withdraw.WithdrawResponse.getDefaultInstance();
    pb.withdraw.Withdraw.WithdrawResponse.Builder b = pb.withdraw.Withdraw.WithdrawResponse.newBuilder()
        .setWithdrawId(model.getId() != null ? model.getId() : 0)
        .setWithdrawNo(model.getWithdrawNo() != null ? model.getWithdrawNo() : "")
        .setCardNumber(model.getCardNumber() != null ? model.getCardNumber() : "")
        .setWithdrawAmount(model.getWithdrawAmount() != null ? model.getWithdrawAmount().intValue() : 0);

    if (model.getWithdrawTime() != null)
      b.setWithdrawTime(model.getWithdrawTime().toString());
    if (model.getCreatedAt() != null)
      b.setCreatedAt(model.getCreatedAt().toString());
    if (model.getUpdatedAt() != null)
      b.setUpdatedAt(model.getUpdatedAt().toString());

    return b.build();
  }

  public static pb.withdraw.Withdraw.WithdrawResponseDeleteAt toResponseDeleted(Withdraw model) {
    if (model == null)
      return pb.withdraw.Withdraw.WithdrawResponseDeleteAt.getDefaultInstance();
    pb.withdraw.Withdraw.WithdrawResponseDeleteAt.Builder b = pb.withdraw.Withdraw.WithdrawResponseDeleteAt.newBuilder()
        .setWithdrawId(model.getId() != null ? model.getId() : 0)
        .setWithdrawNo(model.getWithdrawNo() != null ? model.getWithdrawNo() : "")
        .setCardNumber(model.getCardNumber() != null ? model.getCardNumber() : "")
        .setWithdrawAmount(model.getWithdrawAmount() != null ? model.getWithdrawAmount().intValue() : 0);

    if (model.getWithdrawTime() != null)
      b.setWithdrawTime(model.getWithdrawTime().toString());
    if (model.getCreatedAt() != null)
      b.setCreatedAt(model.getCreatedAt().toString());
    if (model.getUpdatedAt() != null)
      b.setUpdatedAt(model.getUpdatedAt().toString());
    if (model.getDeletedAt() != null)
      b.setDeletedAt(com.google.protobuf.StringValue.of(model.getDeletedAt().toString()));

    return b.build();
  }

  public static pb.withdraw.Withdraw.WithdrawResponse fromWithdrawResponse(WithdrawResponse model) {
    if (model == null)
      return pb.withdraw.Withdraw.WithdrawResponse.getDefaultInstance();
    return pb.withdraw.Withdraw.WithdrawResponse.newBuilder()
        .setWithdrawId(model.getId() != null ? model.getId() : 0)
        .setCardNumber(model.getCardNumber() != null ? model.getCardNumber() : "")
        .setWithdrawAmount(model.getWithdrawAmount() != null ? model.getWithdrawAmount() : 0)
        .setWithdrawTime(model.getWithdrawTime() != null ? model.getWithdrawTime() : "")
        .setCreatedAt(model.getCreatedAt() != null ? model.getCreatedAt() : "")
        .setUpdatedAt(model.getUpdatedAt() != null ? model.getUpdatedAt() : "")
        .build();
  }

  public static pb.withdraw.Withdraw.WithdrawResponseDeleteAt fromWithdrawResponseDeleteAt(
      WithdrawResponseDeleteAt model) {
    if (model == null)
      return pb.withdraw.Withdraw.WithdrawResponseDeleteAt.getDefaultInstance();
    var b = pb.withdraw.Withdraw.WithdrawResponseDeleteAt.newBuilder()
        .setWithdrawId(model.getId() != null ? model.getId() : 0)
        .setCardNumber(model.getCardNumber() != null ? model.getCardNumber() : "")
        .setWithdrawAmount(model.getWithdrawAmount() != null ? model.getWithdrawAmount() : 0)
        .setWithdrawTime(model.getWithdrawTime() != null ? model.getWithdrawTime() : "")
        .setCreatedAt(model.getCreatedAt() != null ? model.getCreatedAt() : "")
        .setUpdatedAt(model.getUpdatedAt() != null ? model.getUpdatedAt() : "");

    if (model.getDeletedAt() != null) {
      b.setDeletedAt(com.google.protobuf.StringValue.of(model.getDeletedAt()));
    }
    return b.build();
  }

  public static pb.withdraw.stats.WithdrawStatsAmount.WithdrawMonthlyAmountResponse toMonthAmount(
      io.example.withdraw.model.WithdrawStats.MonthAmount src) {
    return pb.withdraw.stats.WithdrawStatsAmount.WithdrawMonthlyAmountResponse.newBuilder()
        .setMonth(src.getMonth() != null ? src.getMonth() : "")
        .setTotalAmount(src.getTotalAmount() != null ? src.getTotalAmount().intValue() : 0)
        .build();
  }

  public static pb.withdraw.stats.WithdrawStatsAmount.WithdrawYearlyAmountResponse toYearlyAmount(
      io.example.withdraw.model.WithdrawStats.YearAmount src) {
    return pb.withdraw.stats.WithdrawStatsAmount.WithdrawYearlyAmountResponse.newBuilder()
        .setYear(src.getYear() != null ? src.getYear() : "")
        .setTotalAmount(src.getTotalAmount() != null ? src.getTotalAmount().intValue() : 0)
        .build();
  }

  public static pb.withdraw.stats.WithdrawStatsStatus.WithdrawMonthStatusSuccessResponse toMonthSuccess(
      io.example.withdraw.model.WithdrawStats.MonthStatus src) {
    return pb.withdraw.stats.WithdrawStatsStatus.WithdrawMonthStatusSuccessResponse.newBuilder()
        .setYear(src.getYear() != null ? src.getYear() : "")
        .setMonth(src.getMonth() != null ? src.getMonth() : "")
        .setTotalSuccess(src.getTotalCount() != null ? src.getTotalCount().intValue() : 0)
        .setTotalAmount(src.getTotalAmount() != null ? src.getTotalAmount().intValue() : 0)
        .build();
  }

  public static pb.withdraw.stats.WithdrawStatsStatus.WithdrawYearStatusSuccessResponse toYearlySuccess(
      io.example.withdraw.model.WithdrawStats.YearStatus src) {
    return pb.withdraw.stats.WithdrawStatsStatus.WithdrawYearStatusSuccessResponse.newBuilder()
        .setYear(src.getYear() != null ? src.getYear() : "")
        .setTotalSuccess(src.getTotalCount() != null ? src.getTotalCount().intValue() : 0)
        .setTotalAmount(src.getTotalAmount() != null ? src.getTotalAmount().intValue() : 0)
        .build();
  }

  public static pb.withdraw.stats.WithdrawStatsStatus.WithdrawMonthStatusFailedResponse toMonthFailed(
      io.example.withdraw.model.WithdrawStats.MonthStatus src) {
    return pb.withdraw.stats.WithdrawStatsStatus.WithdrawMonthStatusFailedResponse.newBuilder()
        .setYear(src.getYear() != null ? src.getYear() : "")
        .setMonth(src.getMonth() != null ? src.getMonth() : "")
        .setTotalFailed(src.getTotalCount() != null ? src.getTotalCount().intValue() : 0)
        .setTotalAmount(src.getTotalAmount() != null ? src.getTotalAmount().intValue() : 0)
        .build();
  }

  public static pb.withdraw.stats.WithdrawStatsStatus.WithdrawYearStatusFailedResponse toYearlyFailed(
      io.example.withdraw.model.WithdrawStats.YearStatus src) {
    return pb.withdraw.stats.WithdrawStatsStatus.WithdrawYearStatusFailedResponse.newBuilder()
        .setYear(src.getYear() != null ? src.getYear() : "")
        .setTotalFailed(src.getTotalCount() != null ? src.getTotalCount().intValue() : 0)
        .setTotalAmount(src.getTotalAmount() != null ? src.getTotalAmount().intValue() : 0)
        .build();
  }

}
