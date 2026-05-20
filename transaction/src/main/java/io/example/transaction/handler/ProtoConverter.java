package io.example.transaction.handler;

import io.example.transaction.model.Transaction;
import io.example.transaction.model.TransactionResponse;
import io.example.transaction.model.TransactionResponseDeleteAt;
import java.time.format.DateTimeFormatter;

public final class ProtoConverter {
  private ProtoConverter() {
  }

  public static pb.transaction.Transaction.TransactionResponse toProto(Transaction model) {
    if (model == null)
      return pb.transaction.Transaction.TransactionResponse.getDefaultInstance();

    var builder = pb.transaction.Transaction.TransactionResponse.newBuilder()
        .setId(model.getId() != null ? model.getId() : 0)
        .setTransactionNo(model.getTransactionNo() != null ? model.getTransactionNo() : "")
        .setCardNumber(model.getCardNumber() != null ? model.getCardNumber() : "")
        .setAmount(model.getAmount() != null ? model.getAmount().intValue() : 0)
        .setPaymentMethod(model.getPaymentMethod() != null ? model.getPaymentMethod() : "")
        .setMerchantId(model.getMerchantId() != null ? model.getMerchantId() : 0);

    if (model.getTransactionTime() != null) {
      builder.setTransactionTime(model.getTransactionTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }
    if (model.getCreatedAt() != null) {
      builder.setCreatedAt(model.getCreatedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }
    if (model.getUpdatedAt() != null) {
      builder.setUpdatedAt(model.getUpdatedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    return builder.build();
  }

  public static pb.transaction.Transaction.TransactionResponseDeleteAt toProtoDeleteAt(Transaction model) {
    if (model == null)
      return pb.transaction.Transaction.TransactionResponseDeleteAt.getDefaultInstance();

    var builder = pb.transaction.Transaction.TransactionResponseDeleteAt.newBuilder()
        .setId(model.getId() != null ? model.getId() : 0)
        .setTransactionNo(model.getTransactionNo() != null ? model.getTransactionNo() : "")
        .setCardNumber(model.getCardNumber() != null ? model.getCardNumber() : "")
        .setAmount(model.getAmount() != null ? model.getAmount().intValue() : 0)
        .setPaymentMethod(model.getPaymentMethod() != null ? model.getPaymentMethod() : "")
        .setMerchantId(model.getMerchantId() != null ? model.getMerchantId() : 0);

    if (model.getTransactionTime() != null) {
      builder.setTransactionTime(model.getTransactionTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }
    if (model.getCreatedAt() != null) {
      builder.setCreatedAt(model.getCreatedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }
    if (model.getUpdatedAt() != null) {
      builder.setUpdatedAt(model.getUpdatedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }
    if (model.getDeletedAt() != null) {
      builder.setDeletedAt(
          com.google.protobuf.StringValue.of(model.getDeletedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
    }

    return builder.build();
  }

  public static pb.transaction.Transaction.TransactionResponse fromTransactionResponse(TransactionResponse model) {
    if (model == null)
      return pb.transaction.Transaction.TransactionResponse.getDefaultInstance();
    return pb.transaction.Transaction.TransactionResponse.newBuilder()
        .setId(model.getId() != null ? model.getId() : 0)
        .setCardNumber(model.getCardNumber() != null ? model.getCardNumber() : "")
        .setAmount(model.getAmount() != null ? model.getAmount() : 0)
        .setPaymentMethod(model.getPaymentMethod() != null ? model.getPaymentMethod() : "")
        .setMerchantId(model.getMerchantId() != null ? model.getMerchantId() : 0)
        .setTransactionTime(model.getTransactionTime() != null ? model.getTransactionTime() : "")
        .setCreatedAt(model.getCreatedAt() != null ? model.getCreatedAt() : "")
        .setUpdatedAt(model.getUpdatedAt() != null ? model.getUpdatedAt() : "")
        .build();
  }

  public static pb.transaction.Transaction.TransactionResponseDeleteAt fromTransactionResponseDeleteAt(
      TransactionResponseDeleteAt model) {
    if (model == null)
      return pb.transaction.Transaction.TransactionResponseDeleteAt.getDefaultInstance();
    var builder = pb.transaction.Transaction.TransactionResponseDeleteAt.newBuilder()
        .setId(model.getId() != null ? model.getId() : 0)
        .setCardNumber(model.getCardNumber() != null ? model.getCardNumber() : "")
        .setAmount(model.getAmount() != null ? model.getAmount() : 0)
        .setPaymentMethod(model.getPaymentMethod() != null ? model.getPaymentMethod() : "")
        .setMerchantId(model.getMerchantId() != null ? model.getMerchantId() : 0)
        .setTransactionTime(model.getTransactionTime() != null ? model.getTransactionTime() : "")
        .setCreatedAt(model.getCreatedAt() != null ? model.getCreatedAt() : "")
        .setUpdatedAt(model.getUpdatedAt() != null ? model.getUpdatedAt() : "");

    if (model.getDeletedAt() != null) {
      builder.setDeletedAt(com.google.protobuf.StringValue.of(model.getDeletedAt()));
    }

    return builder.build();
  }
}
