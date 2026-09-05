package io.example.card.repository.impl;

import io.example.card.model.CardReward;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class CardRewardRepositoryImplTest {

  @Mock
  private Pool pool;

  @Mock
  private PreparedQuery<RowSet<Row>> preparedQuery;

  @Mock
  private RowSet<Row> rowSet;

  @Mock
  private RowIterator<Row> iterator;

  @Mock
  private Row row;

  private CardRewardRepositoryImpl repo;
  private final UUID txnUuid = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    repo = new CardRewardRepositoryImpl(pool);
  }

  private void mockPool() {
    lenient().when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockRewardRow() {
    lenient().when(row.getInteger("reward_id")).thenReturn(1);
    lenient().when(row.getString("card_number")).thenReturn("4111111111111111");
    lenient().when(row.getUUID("txn_id")).thenReturn(txnUuid);
    lenient().when(row.getString("reward_type")).thenReturn("POINTS");
    lenient().when(row.getLong("amount")).thenReturn(100L);
    lenient().when(row.getString("description")).thenReturn("Earned points");
    lenient().when(row.getLocalDate("expires_at")).thenReturn(LocalDate.of(2027, 6, 26));
    lenient().when(rowSet.iterator()).thenReturn(iterator);
  }

  private void stubSingleRow() {
    lenient().when(iterator.hasNext()).thenReturn(true, false);
    lenient().when(iterator.next()).thenReturn(row);
    lenient().doAnswer(invocation -> {
      java.util.function.Consumer<Row> consumer = invocation.getArgument(0);
      consumer.accept(row);
      return null;
    }).when(rowSet).forEach(any(java.util.function.Consumer.class));
  }

  @Test
  @DisplayName("addReward inserts reward and returns it")
  void addRewardSuccess(VertxTestContext ctx) {
    mockPool();
    mockRewardRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    CardReward reward = CardReward.builder()
        .cardNumber("4111111111111111")
        .txnId(txnUuid)
        .rewardType("POINTS")
        .amount(100L)
        .description("Earned points")
        .expiresAt(LocalDate.of(2027, 6, 26))
        .build();

    repo.addReward(reward)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getRewardId()).isEqualTo(1);
          assertThat(result.getAmount()).isEqualTo(100L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getBalance returns sum of reward amounts")
  void getBalanceSuccess(VertxTestContext ctx) {
    mockPool();
    lenient().when(rowSet.iterator()).thenReturn(iterator);
    lenient().when(iterator.next()).thenReturn(row);
    lenient().when(row.getLong("balance")).thenReturn(500L);
    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getBalance("4111111111111111")
        .onComplete(ctx.succeeding(balance -> ctx.verify(() -> {
          assertThat(balance).isEqualTo(500L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getHistory returns history of rewards")
  void getHistorySuccess(VertxTestContext ctx) {
    mockPool();
    mockRewardRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getHistory("4111111111111111")
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("redeemRewards inserts negative reward points and returns 1L")
  void redeemRewardsSuccess(VertxTestContext ctx) {
    mockPool();
    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.redeemRewards("4111111111111111", 100L, "Reward redemption")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isEqualTo(1L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getExpiringRewards returns expiring rewards within range")
  void getExpiringRewardsSuccess(VertxTestContext ctx) {
    mockPool();
    mockRewardRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getExpiringRewards(30)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          ctx.completeNow();
        })));
  }
}
