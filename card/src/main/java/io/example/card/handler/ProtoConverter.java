package io.example.card.handler;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import com.google.protobuf.StringValue;
import io.example.card.model.Card;
import io.example.card.model.CardEmail;
import io.example.card.model.CardResponse;
import io.example.card.model.CardResponseDeleteAt;

public class ProtoConverter {

  private static String formatTimestamp(Timestamp ts) {
    return ts != null ? ts.toInstant().toString() : "";
  }

  public static pb.card.Card.CardResponse toResponse(Card card) {
    if (card == null) return pb.card.Card.CardResponse.getDefaultInstance();
    return pb.card.Card.CardResponse.newBuilder()
        .setId(card.getId() != null ? card.getId() : 0)
        .setUserId(card.getUserId() != null ? card.getUserId() : 0)
        .setCardNumber(card.getCardNumber() != null ? card.getCardNumber() : "")
        .setCardType(card.getCardType() != null ? card.getCardType() : "")
        .setExpireDate(card.getExpireDate() != null ? card.getExpireDate() : "")
        .setCvv(card.getCvv() != null ? card.getCvv() : "")
        .setCardProvider(card.getCardProvider() != null ? card.getCardProvider() : "")
        .setCreatedAt(formatTimestamp(card.getCreatedAt()))
        .setUpdatedAt(formatTimestamp(card.getUpdatedAt()))
        .build();
  }

  public static pb.card.Card.CardResponseDeleteAt toResponseDeleted(Card card) {
    if (card == null) return pb.card.Card.CardResponseDeleteAt.getDefaultInstance();
    pb.card.Card.CardResponseDeleteAt.Builder b = pb.card.Card.CardResponseDeleteAt.newBuilder()
        .setId(card.getId() != null ? card.getId() : 0)
        .setUserId(card.getUserId() != null ? card.getUserId() : 0)
        .setCardNumber(card.getCardNumber() != null ? card.getCardNumber() : "")
        .setCardType(card.getCardType() != null ? card.getCardType() : "")
        .setExpireDate(card.getExpireDate() != null ? card.getExpireDate() : "")
        .setCvv(card.getCvv() != null ? card.getCvv() : "")
        .setCardProvider(card.getCardProvider() != null ? card.getCardProvider() : "")
        .setCreatedAt(formatTimestamp(card.getCreatedAt()))
        .setUpdatedAt(formatTimestamp(card.getUpdatedAt()));

    if (card.getDeletedAt() != null) {
      b.setDeletedAt(StringValue.of(formatTimestamp(card.getDeletedAt())));
    }
    return b.build();
  }

  public static pb.card.Card.CardWithEmailResponse toEmailResponse(CardEmail ce) {
    if (ce == null) return pb.card.Card.CardWithEmailResponse.getDefaultInstance();
    return pb.card.Card.CardWithEmailResponse.newBuilder()
        .setId(ce.getId() != null ? ce.getId() : 0)
        .setUserId(ce.getUserId() != null ? ce.getUserId() : 0)
        .setEmail(ce.getEmail() != null ? ce.getEmail() : "")
        .setCardNumber(ce.getCardNumber() != null ? ce.getCardNumber() : "")
        .setCardType(ce.getCardType() != null ? ce.getCardType() : "")
        .setExpireDate(ce.getExpireDate() != null ? ce.getExpireDate() : "")
        .setCvv(ce.getCvv() != null ? ce.getCvv() : "")
        .setCardProvider(ce.getCardProvider() != null ? ce.getCardProvider() : "")
        .setCreatedAt(formatTimestamp(ce.getCreatedAt()))
        .setUpdatedAt(formatTimestamp(ce.getUpdatedAt()))
        .build();
  }

  public static String formatExpDate(com.google.protobuf.Timestamp protoTs) {
    if (protoTs == null || (protoTs.getSeconds() == 0 && protoTs.getNanos() == 0)) {
      return Instant.now().toString().split("T")[0];
    }
    return Instant.ofEpochSecond(protoTs.getSeconds(), protoTs.getNanos())
        .atZone(java.time.ZoneOffset.UTC)
        .format(DateTimeFormatter.ISO_LOCAL_DATE);
  }

  public static pb.card.Card.CardResponse fromResponse(CardResponse cr) {
    if (cr == null) return pb.card.Card.CardResponse.getDefaultInstance();
    return pb.card.Card.CardResponse.newBuilder()
        .setId(cr.getId() != null ? cr.getId() : 0)
        .setUserId(cr.getUserId() != null ? cr.getUserId() : 0)
        .setCardNumber(cr.getCardNumber() != null ? cr.getCardNumber() : "")
        .setCardType(cr.getCardType() != null ? cr.getCardType() : "")
        .setExpireDate(cr.getExpireDate() != null ? cr.getExpireDate() : "")
        .setCvv(cr.getCvv() != null ? cr.getCvv() : "")
        .setCardProvider(cr.getCardProvider() != null ? cr.getCardProvider() : "")
        .setCreatedAt(cr.getCreatedAt() != null ? cr.getCreatedAt() : "")
        .setUpdatedAt(cr.getUpdatedAt() != null ? cr.getUpdatedAt() : "")
        .build();
  }

  public static pb.card.Card.CardResponseDeleteAt fromResponseDeleted(CardResponseDeleteAt crda) {
    if (crda == null) return pb.card.Card.CardResponseDeleteAt.getDefaultInstance();
    pb.card.Card.CardResponseDeleteAt.Builder b = pb.card.Card.CardResponseDeleteAt.newBuilder()
        .setId(crda.getId() != null ? crda.getId() : 0)
        .setUserId(crda.getUserId() != null ? crda.getUserId() : 0)
        .setCardNumber(crda.getCardNumber() != null ? crda.getCardNumber() : "")
        .setCardType(crda.getCardType() != null ? crda.getCardType() : "")
        .setExpireDate(crda.getExpireDate() != null ? crda.getExpireDate() : "")
        .setCvv(crda.getCvv() != null ? crda.getCvv() : "")
        .setCardProvider(crda.getCardProvider() != null ? crda.getCardProvider() : "")
        .setCreatedAt(crda.getCreatedAt() != null ? crda.getCreatedAt() : "")
        .setUpdatedAt(crda.getUpdatedAt() != null ? crda.getUpdatedAt() : "");

    if (crda.getDeletedAt() != null) {
      b.setDeletedAt(StringValue.of(crda.getDeletedAt()));
    }
    return b.build();
  }
}
