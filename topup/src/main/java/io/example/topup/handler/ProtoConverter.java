package io.example.topup.handler;

import com.google.protobuf.StringValue;

import io.example.topup.model.Topup;
import io.example.topup.model.TopupResponse;
import io.example.topup.model.TopupResponseDeleteAt;

public class ProtoConverter {

  public static pb.topup.Topup.TopupResponse toResponse(Topup model) {
    if (model == null)
      return pb.topup.Topup.TopupResponse.getDefaultInstance();
    pb.topup.Topup.TopupResponse.Builder b = pb.topup.Topup.TopupResponse.newBuilder()
        .setId(model.getId() != null ? model.getId() : 0)
        .setCardNumber(model.getCardNumber() != null ? model.getCardNumber() : "")
        .setTopupNo(model.getTopupNo() != null ? model.getTopupNo() : "")
        .setTopupAmount(model.getTopupAmount() != null ? model.getTopupAmount().intValue() : 0)
        .setTopupMethod(model.getTopupMethod() != null ? model.getTopupMethod() : "");

    if (model.getTopupTime() != null)
      b.setTopupTime(model.getTopupTime().toString());
    if (model.getCreatedAt() != null)
      b.setCreatedAt(model.getCreatedAt().toString());
    if (model.getUpdatedAt() != null)
      b.setUpdatedAt(model.getUpdatedAt().toString());

    return b.build();
  }

  public static pb.topup.Topup.TopupResponseDeleteAt toResponseDeleted(Topup model) {
    if (model == null)
      return pb.topup.Topup.TopupResponseDeleteAt.getDefaultInstance();
    pb.topup.Topup.TopupResponseDeleteAt.Builder b = pb.topup.Topup.TopupResponseDeleteAt.newBuilder()
        .setId(model.getId() != null ? model.getId() : 0)
        .setCardNumber(model.getCardNumber() != null ? model.getCardNumber() : "")
        .setTopupNo(model.getTopupNo() != null ? model.getTopupNo() : "")
        .setTopupAmount(model.getTopupAmount() != null ? model.getTopupAmount().intValue() : 0)
        .setTopupMethod(model.getTopupMethod() != null ? model.getTopupMethod() : "");

    if (model.getTopupTime() != null)
      b.setTopupTime(model.getTopupTime().toString());
    if (model.getCreatedAt() != null)
      b.setCreatedAt(model.getCreatedAt().toString());
    if (model.getUpdatedAt() != null)
      b.setUpdatedAt(model.getUpdatedAt().toString());
    if (model.getDeletedAt() != null)
      b.setDeletedAt(StringValue.of(model.getDeletedAt().toString()));

    return b.build();
  }

  public static pb.topup.Topup.TopupResponse fromTopupResponse(TopupResponse model) {
    if (model == null)
      return pb.topup.Topup.TopupResponse.getDefaultInstance();
    return pb.topup.Topup.TopupResponse.newBuilder()
        .setId(model.getId() != null ? model.getId() : 0)
        .setCardNumber(model.getCardNumber() != null ? model.getCardNumber() : "")
        .setTopupAmount(model.getAmount() != null ? model.getAmount() : 0)
        .setCreatedAt(model.getCreatedAt() != null ? model.getCreatedAt() : "")
        .setUpdatedAt(model.getUpdatedAt() != null ? model.getUpdatedAt() : "")
        .build();
  }

  public static pb.topup.Topup.TopupResponseDeleteAt fromTopupResponseToProtoDeleteAt(TopupResponse model) {
    if (model == null)
      return pb.topup.Topup.TopupResponseDeleteAt.getDefaultInstance();
    return pb.topup.Topup.TopupResponseDeleteAt.newBuilder()
        .setId(model.getId() != null ? model.getId() : 0)
        .setCardNumber(model.getCardNumber() != null ? model.getCardNumber() : "")
        .setTopupAmount(model.getAmount() != null ? model.getAmount() : 0)
        .setCreatedAt(model.getCreatedAt() != null ? model.getCreatedAt() : "")
        .setUpdatedAt(model.getUpdatedAt() != null ? model.getUpdatedAt() : "")
        .build();
  }

  public static pb.topup.Topup.TopupResponseDeleteAt fromTopupResponseDeleteAt(TopupResponseDeleteAt model) {
    if (model == null)
      return pb.topup.Topup.TopupResponseDeleteAt.getDefaultInstance();
    pb.topup.Topup.TopupResponseDeleteAt.Builder b = pb.topup.Topup.TopupResponseDeleteAt.newBuilder()
        .setId(model.getId() != null ? model.getId() : 0)
        .setCardNumber(model.getCardNumber() != null ? model.getCardNumber() : "")
        .setTopupAmount(model.getAmount() != null ? model.getAmount() : 0)
        .setCreatedAt(model.getCreatedAt() != null ? model.getCreatedAt() : "")
        .setUpdatedAt(model.getUpdatedAt() != null ? model.getUpdatedAt() : "");

    if (model.getDeletedAt() != null)
      b.setDeletedAt(StringValue.of(model.getDeletedAt()));

    return b.build();
  }

  public static pb.topup.stats.TopupStatsAmount.TopupMonthAmountResponse toMonthAmount(
      io.example.topup.model.TopupStats.MonthAmount src) {
    return pb.topup.stats.TopupStatsAmount.TopupMonthAmountResponse.newBuilder()
        .setMonth(src.getMonth() != null ? src.getMonth() : "")
        .setTotalAmount(src.getTotalAmount() != null ? src.getTotalAmount().intValue() : 0)
        .build();
  }

  public static pb.topup.stats.TopupStatsAmount.TopupYearlyAmountResponse toYearlyAmount(
      io.example.topup.model.TopupStats.YearAmount src) {
    return pb.topup.stats.TopupStatsAmount.TopupYearlyAmountResponse.newBuilder()
        .setYear(src.getYear() != null ? src.getYear() : "")
        .setTotalAmount(src.getTotalAmount() != null ? src.getTotalAmount().intValue() : 0)
        .build();
  }

  public static pb.topup.stats.TopupStatsMethod.TopupMonthMethodResponse toMonthMethod(
      io.example.topup.model.TopupStats.MonthMethod src) {
    return pb.topup.stats.TopupStatsMethod.TopupMonthMethodResponse.newBuilder()
        .setMonth(src.getMonth() != null ? src.getMonth() : "")
        .setTopupMethod(src.getTopupMethod() != null ? src.getTopupMethod() : "")
        .setTotalTopups(src.getTotalTopups() != null ? src.getTotalTopups().intValue() : 0)
        .setTotalAmount(src.getTotalAmount() != null ? src.getTotalAmount().intValue() : 0)
        .build();
  }

  public static pb.topup.stats.TopupStatsMethod.TopupYearlyMethodResponse toYearlyMethod(
      io.example.topup.model.TopupStats.YearMethod src) {
    return pb.topup.stats.TopupStatsMethod.TopupYearlyMethodResponse.newBuilder()
        .setYear(src.getYear() != null ? src.getYear() : "")
        .setTopupMethod(src.getTopupMethod() != null ? src.getTopupMethod() : "")
        .setTotalTopups(src.getTotalTopups() != null ? src.getTotalTopups().intValue() : 0)
        .setTotalAmount(src.getTotalAmount() != null ? src.getTotalAmount().intValue() : 0)
        .build();
  }

  public static pb.topup.stats.TopupStatsStatus.TopupMonthStatusSuccessResponse toMonthSuccess(
      io.example.topup.model.TopupStats.MonthStatus src) {
    return pb.topup.stats.TopupStatsStatus.TopupMonthStatusSuccessResponse.newBuilder()
        .setYear(src.getYear() != null ? src.getYear() : "")
        .setMonth(src.getMonth() != null ? src.getMonth() : "")
        .setTotalSuccess(src.getTotalCount() != null ? src.getTotalCount().intValue() : 0)
        .setTotalAmount(src.getTotalAmount() != null ? src.getTotalAmount().intValue() : 0)
        .build();
  }

  public static pb.topup.stats.TopupStatsStatus.TopupYearStatusSuccessResponse toYearlySuccess(
      io.example.topup.model.TopupStats.YearStatus src) {
    return pb.topup.stats.TopupStatsStatus.TopupYearStatusSuccessResponse.newBuilder()
        .setYear(src.getYear() != null ? src.getYear() : "")
        .setTotalSuccess(src.getTotalCount() != null ? src.getTotalCount().intValue() : 0)
        .setTotalAmount(src.getTotalAmount() != null ? src.getTotalAmount().intValue() : 0)
        .build();
  }

  public static pb.topup.stats.TopupStatsStatus.TopupMonthStatusFailedResponse toMonthFailed(
      io.example.topup.model.TopupStats.MonthStatus src) {
    return pb.topup.stats.TopupStatsStatus.TopupMonthStatusFailedResponse.newBuilder()
        .setYear(src.getYear() != null ? src.getYear() : "")
        .setMonth(src.getMonth() != null ? src.getMonth() : "")
        .setTotalFailed(src.getTotalCount() != null ? src.getTotalCount().intValue() : 0)
        .setTotalAmount(src.getTotalAmount() != null ? src.getTotalAmount().intValue() : 0)
        .build();
  }

  public static pb.topup.stats.TopupStatsStatus.TopupYearStatusFailedResponse toYearlyFailed(
      io.example.topup.model.TopupStats.YearStatus src) {
    return pb.topup.stats.TopupStatsStatus.TopupYearStatusFailedResponse.newBuilder()
        .setYear(src.getYear() != null ? src.getYear() : "")
        .setTotalFailed(src.getTotalCount() != null ? src.getTotalCount().intValue() : 0)
        .setTotalAmount(src.getTotalAmount() != null ? src.getTotalAmount().intValue() : 0)
        .build();
  }
}
