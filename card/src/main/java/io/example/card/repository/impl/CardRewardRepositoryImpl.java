package io.example.card.repository.impl;

import io.example.card.model.CardReward;
import io.example.card.repository.CardRewardRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class CardRewardRepositoryImpl implements CardRewardRepository {
  private final Pool pool;

  @Override
  public Future<CardReward> addReward(CardReward reward) {
    return pool
        .preparedQuery(
            """
                INSERT INTO card_rewards
                  (card_number, txn_id, reward_type, amount, description, expires_at)
                VALUES ($1, $2::uuid, $3, $4, $5, $6)
                RETURNING *
                """)
        .execute(Tuple.of(
            reward.getCardNumber(),
            reward.getTxnId() != null ? reward.getTxnId().toString() : null,
            reward.getRewardType(),
            reward.getAmount(),
            reward.getDescription(),
            reward.getExpiresAt()))
        .map(this::mapSingleOrNull);
  }

  @Override
  public Future<Long> getBalance(String cardNumber) {
    return pool
        .preparedQuery(
            "SELECT COALESCE(SUM(amount), 0) AS balance FROM card_rewards WHERE card_number = $1")
        .execute(Tuple.of(cardNumber))
        .map(rows -> rows.iterator().next().getLong("balance"));
  }

  @Override
  public Future<List<CardReward>> getHistory(String cardNumber) {
    return pool
        .preparedQuery(
            "SELECT * FROM card_rewards WHERE card_number = $1 ORDER BY created_at DESC")
        .execute(Tuple.of(cardNumber))
        .map(this::mapToList);
  }

  @Override
  public Future<Long> redeemRewards(String cardNumber, Long points, String description) {
    // Insert a negative reward entry to represent redemption
    return pool
        .preparedQuery(
            """
                INSERT INTO card_rewards (card_number, reward_type, amount, description)
                VALUES ($1, 'POINTS', $2, $3)
                RETURNING *
                """)
        .execute(Tuple.of(cardNumber, -points, description))
        .map(rows -> 1L);
  }

  @Override
  public Future<List<CardReward>> getExpiringRewards(int days) {
    return pool
        .preparedQuery(
            """
                SELECT * FROM card_rewards
                WHERE expires_at IS NOT NULL
                  AND expires_at <= CURRENT_DATE + $1
                  AND expires_at > CURRENT_DATE
                ORDER BY expires_at ASC
                """)
        .execute(Tuple.of(days))
        .map(this::mapToList);
  }

  private CardReward mapSingleOrNull(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? CardReward.fromRow(rows.iterator().next()) : null;
  }

  private List<CardReward> mapToList(RowSet<Row> rows) {
    List<CardReward> list = new ArrayList<>();
    rows.forEach(row -> list.add(CardReward.fromRow(row)));
    return list;
  }
}
