package io.example.merchant.handler;

import java.sql.Timestamp;
import com.google.protobuf.StringValue;
import io.example.merchant.model.Merchant;
import io.example.merchant.model.MerchantDocument;
import io.example.merchant.model.MerchantDocumentResponse;
import io.example.merchant.model.MerchantDocumentResponseDeleteAt;
import io.example.merchant.model.MerchantResponse;
import io.example.merchant.model.MerchantResponseDeleteAt;
import io.example.merchant.model.MerchantTransactions;

public class ProtoConverter {

  private static String formatTimestamp(Timestamp ts) {
    return ts != null ? ts.toInstant().toString() : "";
  }

  public static pb.merchant.Merchant.MerchantResponse toMerchantResponse(Merchant m) {
    if (m == null) return pb.merchant.Merchant.MerchantResponse.getDefaultInstance();
    return pb.merchant.Merchant.MerchantResponse.newBuilder()
        .setId(m.getId() != null ? m.getId() : 0)
        .setName(m.getName() != null ? m.getName() : "")
        .setApiKey(m.getApiKey() != null ? m.getApiKey() : "")
        .setStatus(m.getStatus() != null ? m.getStatus() : "")
        .setUserId(m.getUserId() != null ? m.getUserId() : 0)
        .setCreatedAt(formatTimestamp(m.getCreatedAt()))
        .setUpdatedAt(formatTimestamp(m.getUpdatedAt()))
        .build();
  }

  public static pb.merchant.Merchant.MerchantResponseDeleteAt toMerchantDeleteAt(Merchant m) {
    if (m == null) return pb.merchant.Merchant.MerchantResponseDeleteAt.getDefaultInstance();
    pb.merchant.Merchant.MerchantResponseDeleteAt.Builder b = pb.merchant.Merchant.MerchantResponseDeleteAt.newBuilder()
        .setId(m.getId() != null ? m.getId() : 0)
        .setName(m.getName() != null ? m.getName() : "")
        .setApiKey(m.getApiKey() != null ? m.getApiKey() : "")
        .setStatus(m.getStatus() != null ? m.getStatus() : "")
        .setUserId(m.getUserId() != null ? m.getUserId() : 0)
        .setCreatedAt(formatTimestamp(m.getCreatedAt()))
        .setUpdatedAt(formatTimestamp(m.getUpdatedAt()));
    
    if (m.getDeletedAt() != null) {
      b.setDeletedAt(StringValue.of(formatTimestamp(m.getDeletedAt())));
    }
    return b.build();
  }

  public static pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument toDocumentResponse(MerchantDocument d) {
    if (d == null) return pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument.getDefaultInstance();
    return pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument.newBuilder()
        .setDocumentId(d.getId() != null ? d.getId() : 0)
        .setMerchantId(d.getMerchantId() != null ? d.getMerchantId() : 0)
        .setDocumentType(d.getDocumentType() != null ? d.getDocumentType() : "")
        .setDocumentUrl(d.getDocumentUrl() != null ? d.getDocumentUrl() : "")
        .setStatus(d.getStatus() != null ? d.getStatus() : "")
        .setNote(d.getNote() != null ? d.getNote() : "")
        .setUploadedAt(formatTimestamp(d.getCreatedAt()))
        .setUpdatedAt(formatTimestamp(d.getUpdatedAt()))
        .build();
  }

  public static pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt toDocumentDeleteAt(MerchantDocument d) {
    if (d == null) return pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt.getDefaultInstance();
    pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt.Builder b = pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt.newBuilder()
        .setDocumentId(d.getId() != null ? d.getId() : 0)
        .setMerchantId(d.getMerchantId() != null ? d.getMerchantId() : 0)
        .setDocumentType(d.getDocumentType() != null ? d.getDocumentType() : "")
        .setDocumentUrl(d.getDocumentUrl() != null ? d.getDocumentUrl() : "")
        .setStatus(d.getStatus() != null ? d.getStatus() : "")
        .setNote(d.getNote() != null ? d.getNote() : "")
        .setUploadedAt(formatTimestamp(d.getCreatedAt()))
        .setUpdatedAt(formatTimestamp(d.getUpdatedAt()));
    
    if (d.getDeletedAt() != null) {
      b.setDeletedAt(StringValue.of(formatTimestamp(d.getDeletedAt())));
    }
    return b.build();
  }

  public static pb.merchant.MerchantTransaction.MerchantTransactionResponse toTxnResponse(MerchantTransactions t) {
    if (t == null) return pb.merchant.MerchantTransaction.MerchantTransactionResponse.getDefaultInstance();
    pb.merchant.MerchantTransaction.MerchantTransactionResponse.Builder b = pb.merchant.MerchantTransaction.MerchantTransactionResponse.newBuilder()
        .setId(t.getTransactionId() != null ? t.getTransactionId() : 0)
        .setCardNumber(t.getCardNumber() != null ? t.getCardNumber() : "")
        .setAmount(t.getAmount() != null ? t.getAmount().intValue() : 0)
        .setPaymentMethod(t.getPaymentMethod() != null ? t.getPaymentMethod() : "")
        .setMerchantId(t.getMerchantId() != null ? t.getMerchantId() : 0)
        .setMerchantName(t.getMerchantName() != null ? t.getMerchantName() : "")
        .setTransactionTime(formatTimestamp(t.getTransactionTime()))
        .setCreatedAt(formatTimestamp(t.getCreatedAt()))
        .setUpdatedAt(formatTimestamp(t.getUpdatedAt()));

    if (t.getDeletedAt() != null) {
      b.setDeletedAt(StringValue.of(formatTimestamp(t.getDeletedAt())));
    }
    return b.build();
  }

  public static pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument fromDocumentResponse(MerchantDocumentResponse d) {
    if (d == null) return pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument.getDefaultInstance();
    return pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument.newBuilder()
        .setDocumentId(d.getId() != null ? d.getId() : 0)
        .setMerchantId(d.getMerchantId() != null ? d.getMerchantId() : 0)
        .setDocumentType(d.getDocumentType() != null ? d.getDocumentType() : "")
        .setDocumentUrl(d.getDocumentUrl() != null ? d.getDocumentUrl() : "")
        .setStatus(d.getStatus() != null ? d.getStatus() : "")
        .setNote(d.getNote() != null ? d.getNote() : "")
        .setUploadedAt(d.getCreatedAt() != null ? d.getCreatedAt() : "")
        .setUpdatedAt(d.getUpdatedAt() != null ? d.getUpdatedAt() : "")
        .build();
  }

  public static pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt fromDocumentResponseAt(MerchantDocumentResponseDeleteAt d) {
    if (d == null) return pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt.getDefaultInstance();
    pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt.Builder b = pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt.newBuilder()
        .setDocumentId(d.getId() != null ? d.getId() : 0)
        .setMerchantId(d.getMerchantId() != null ? d.getMerchantId() : 0)
        .setDocumentType(d.getDocumentType() != null ? d.getDocumentType() : "")
        .setDocumentUrl(d.getDocumentUrl() != null ? d.getDocumentUrl() : "")
        .setStatus(d.getStatus() != null ? d.getStatus() : "")
        .setNote(d.getNote() != null ? d.getNote() : "")
        .setUploadedAt(d.getCreatedAt() != null ? d.getCreatedAt() : "")
        .setUpdatedAt(d.getUpdatedAt() != null ? d.getUpdatedAt() : "");

    if (d.getDeletedAt() != null) {
      b.setDeletedAt(StringValue.of(d.getDeletedAt()));
    }
    return b.build();
  }

  public static pb.merchant.Merchant.MerchantResponse fromMerchantResponse(MerchantResponse m) {
    if (m == null) return pb.merchant.Merchant.MerchantResponse.getDefaultInstance();
    return pb.merchant.Merchant.MerchantResponse.newBuilder()
        .setId(m.getId() != null ? m.getId() : 0)
        .setName(m.getName() != null ? m.getName() : "")
        .setApiKey(m.getApiKey() != null ? m.getApiKey() : "")
        .setStatus(m.getStatus() != null ? m.getStatus() : "")
        .setUserId(m.getUserId() != null ? m.getUserId() : 0)
        .setCreatedAt(m.getCreatedAt() != null ? m.getCreatedAt() : "")
        .setUpdatedAt(m.getUpdatedAt() != null ? m.getUpdatedAt() : "")
        .build();
  }

  public static pb.merchant.Merchant.MerchantResponseDeleteAt fromMerchantResponseDeleteAt(MerchantResponseDeleteAt m) {
    if (m == null) return pb.merchant.Merchant.MerchantResponseDeleteAt.getDefaultInstance();
    pb.merchant.Merchant.MerchantResponseDeleteAt.Builder b = pb.merchant.Merchant.MerchantResponseDeleteAt.newBuilder()
        .setId(m.getId() != null ? m.getId() : 0)
        .setName(m.getName() != null ? m.getName() : "")
        .setApiKey(m.getApiKey() != null ? m.getApiKey() : "")
        .setStatus(m.getStatus() != null ? m.getStatus() : "")
        .setUserId(m.getUserId() != null ? m.getUserId() : 0)
        .setCreatedAt(m.getCreatedAt() != null ? m.getCreatedAt() : "")
        .setUpdatedAt(m.getUpdatedAt() != null ? m.getUpdatedAt() : "");

    if (m.getDeletedAt() != null) {
      b.setDeletedAt(StringValue.of(m.getDeletedAt()));
    }
    return b.build();
  }
}
