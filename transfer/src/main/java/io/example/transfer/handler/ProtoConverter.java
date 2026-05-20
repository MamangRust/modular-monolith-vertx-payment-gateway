package io.example.transfer.handler;

import io.example.transfer.model.Transfer;
import io.example.transfer.model.TransferResponse;
import io.example.transfer.model.TransferResponseDeleteAt;

public class ProtoConverter {

  public static pb.transfer.Transfer.TransferResponse toResponse(Transfer model) {
    if (model == null)
      return pb.transfer.Transfer.TransferResponse.getDefaultInstance();
    pb.transfer.Transfer.TransferResponse.Builder b = pb.transfer.Transfer.TransferResponse.newBuilder()
        .setId(model.getId() != null ? model.getId() : 0)
        .setTransferNo(model.getTransferNo() != null ? model.getTransferNo() : "")
        .setTransferFrom(model.getTransferFrom() != null ? model.getTransferFrom() : "")
        .setTransferTo(model.getTransferTo() != null ? model.getTransferTo() : "")
        .setTransferAmount(model.getTransferAmount() != null ? model.getTransferAmount().intValue() : 0);

    if (model.getTransferTime() != null)
      b.setTransferTime(model.getTransferTime().toString());
    if (model.getCreatedAt() != null)
      b.setCreatedAt(model.getCreatedAt().toString());
    if (model.getUpdatedAt() != null)
      b.setUpdatedAt(model.getUpdatedAt().toString());

    return b.build();
  }

  public static pb.transfer.Transfer.TransferResponseDeleteAt toResponseDeleted(Transfer model) {
    if (model == null)
      return pb.transfer.Transfer.TransferResponseDeleteAt.getDefaultInstance();
    pb.transfer.Transfer.TransferResponseDeleteAt.Builder b = pb.transfer.Transfer.TransferResponseDeleteAt.newBuilder()
        .setId(model.getId() != null ? model.getId() : 0)
        .setTransferNo(model.getTransferNo() != null ? model.getTransferNo() : "")
        .setTransferFrom(model.getTransferFrom() != null ? model.getTransferFrom() : "")
        .setTransferTo(model.getTransferTo() != null ? model.getTransferTo() : "")
        .setTransferAmount(model.getTransferAmount() != null ? model.getTransferAmount().intValue() : 0);

    if (model.getTransferTime() != null)
      b.setTransferTime(model.getTransferTime().toString());
    if (model.getCreatedAt() != null)
      b.setCreatedAt(model.getCreatedAt().toString());
    if (model.getUpdatedAt() != null)
      b.setUpdatedAt(model.getUpdatedAt().toString());
    if (model.getDeletedAt() != null)
      b.setDeletedAt(com.google.protobuf.StringValue.of(model.getDeletedAt().toString()));

    return b.build();
  }

  public static pb.transfer.Transfer.TransferResponse fromTransferResponse(TransferResponse model) {
    if (model == null)
      return pb.transfer.Transfer.TransferResponse.getDefaultInstance();
    return pb.transfer.Transfer.TransferResponse.newBuilder()
        .setId(model.getId() != null ? model.getId() : 0)
        .setTransferFrom(model.getTransferFrom() != null ? model.getTransferFrom() : "")
        .setTransferTo(model.getTransferTo() != null ? model.getTransferTo() : "")
        .setTransferAmount(model.getTransferAmount() != null ? model.getTransferAmount().intValue() : 0)
        .setCreatedAt(model.getCreatedAt() != null ? model.getCreatedAt() : "")
        .setUpdatedAt(model.getUpdatedAt() != null ? model.getUpdatedAt() : "")
        .build();
  }

  public static pb.transfer.Transfer.TransferResponseDeleteAt fromTransferResponseDeleteAt(
      TransferResponseDeleteAt model) {
    if (model == null)
      return pb.transfer.Transfer.TransferResponseDeleteAt.getDefaultInstance();
    var b = pb.transfer.Transfer.TransferResponseDeleteAt.newBuilder()
        .setId(model.getId() != null ? model.getId() : 0)
        .setTransferFrom(model.getTransferFrom() != null ? model.getTransferFrom() : "")
        .setTransferTo(model.getTransferTo() != null ? model.getTransferTo() : "")
        .setTransferAmount(model.getTransferAmount() != null ? model.getTransferAmount().intValue() : 0)
        .setCreatedAt(model.getCreatedAt() != null ? model.getCreatedAt() : "")
        .setUpdatedAt(model.getUpdatedAt() != null ? model.getUpdatedAt() : "");

    if (model.getDeletedAt() != null) {
      b.setDeletedAt(com.google.protobuf.StringValue.of(model.getDeletedAt()));
    }
    return b.build();
  }

  public static pb.transfer.stats.TransferStatsAmount.TransferMonthAmountResponse toMonthAmount(
      io.example.transfer.model.TransferStats.MonthAmount src) {
    return pb.transfer.stats.TransferStatsAmount.TransferMonthAmountResponse.newBuilder()
        .setMonth(src.getMonth() != null ? src.getMonth() : "")
        .setTotalAmount(src.getTotalAmount() != null ? src.getTotalAmount().intValue() : 0)
        .build();
  }

  public static pb.transfer.stats.TransferStatsAmount.TransferYearAmountResponse toYearlyAmount(
      io.example.transfer.model.TransferStats.YearAmount src) {
    return pb.transfer.stats.TransferStatsAmount.TransferYearAmountResponse.newBuilder()
        .setYear(src.getYear() != null ? src.getYear() : "")
        .setTotalAmount(src.getTotalAmount() != null ? src.getTotalAmount().intValue() : 0)
        .build();
  }

  public static pb.transfer.stats.TransferStatsStatus.TransferMonthStatusSuccessResponse toMonthSuccess(
      io.example.transfer.model.TransferStats.MonthStatus src) {
    return pb.transfer.stats.TransferStatsStatus.TransferMonthStatusSuccessResponse.newBuilder()
        .setYear(src.getYear() != null ? src.getYear() : "")
        .setMonth(src.getMonth() != null ? src.getMonth() : "")
        .setTotalSuccess(src.getTotalCount() != null ? src.getTotalCount().intValue() : 0)
        .setTotalAmount(src.getTotalAmount() != null ? src.getTotalAmount().intValue() : 0)
        .build();
  }

  public static pb.transfer.stats.TransferStatsStatus.TransferYearStatusSuccessResponse toYearlySuccess(
      io.example.transfer.model.TransferStats.YearStatus src) {
    return pb.transfer.stats.TransferStatsStatus.TransferYearStatusSuccessResponse.newBuilder()
        .setYear(src.getYear() != null ? src.getYear() : "")
        .setTotalSuccess(src.getTotalCount() != null ? src.getTotalCount().intValue() : 0)
        .setTotalAmount(src.getTotalAmount() != null ? src.getTotalAmount().intValue() : 0)
        .build();
  }

  public static pb.transfer.stats.TransferStatsStatus.TransferMonthStatusFailedResponse toMonthFailed(
      io.example.transfer.model.TransferStats.MonthStatus src) {
    return pb.transfer.stats.TransferStatsStatus.TransferMonthStatusFailedResponse.newBuilder()
        .setYear(src.getYear() != null ? src.getYear() : "")
        .setMonth(src.getMonth() != null ? src.getMonth() : "")
        .setTotalFailed(src.getTotalCount() != null ? src.getTotalCount().intValue() : 0)
        .setTotalAmount(src.getTotalAmount() != null ? src.getTotalAmount().intValue() : 0)
        .build();
  }

  public static pb.transfer.stats.TransferStatsStatus.TransferYearStatusFailedResponse toYearlyFailed(
      io.example.transfer.model.TransferStats.YearStatus src) {
    return pb.transfer.stats.TransferStatsStatus.TransferYearStatusFailedResponse.newBuilder()
        .setYear(src.getYear() != null ? src.getYear() : "")
        .setTotalFailed(src.getTotalCount() != null ? src.getTotalCount().intValue() : 0)
        .setTotalAmount(src.getTotalAmount() != null ? src.getTotalAmount().intValue() : 0)
        .build();
  }
}
