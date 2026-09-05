package io.example.card.repository.impl;

import io.example.card.model.CardEventLog;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class CardEventLogRepositoryImplTest {

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

  private CardEventLogRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new CardEventLogRepositoryImpl(pool);
  }

  private void mockPool() {
    lenient().when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockRow() {
    lenient().when(row.getLong("event_id")).thenReturn(1L);
    lenient().when(row.getString("topic")).thenReturn("card.payment.posted");
    lenient().when(row.getString("event_type")).thenReturn("PAYMENT_POSTED");
    lenient().when(row.getString("card_number")).thenReturn("4111111111111111");
    lenient().when(row.getString("reference_id")).thenReturn("ref-123");
    lenient().when(row.get(io.vertx.core.json.JsonObject.class, "payload"))
        .thenReturn(new JsonObject("{\"amount\":100000}"));
    lenient().when(row.getLocalDateTime("received_at")).thenReturn(LocalDateTime.of(2026, 8, 8, 10, 0));
    lenient().when(rowSet.iterator()).thenReturn(iterator);
    lenient().when(iterator.hasNext()).thenReturn(true, false);
    lenient().when(iterator.next()).thenReturn(row);
  }

  @Test
  @DisplayName("insert persists event log and returns mapped row")
  void insertSuccess(VertxTestContext ctx) {
    mockPool();
    mockRow();
    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    CardEventLog eventLog = CardEventLog.builder()
        .topic("card.payment.posted")
        .eventType("PAYMENT_POSTED")
        .cardNumber("4111111111111111")
        .referenceId("ref-123")
        .payload(new JsonObject().put("amount", 100000))
        .build();

    repo.insert(eventLog)
        .onComplete(ctx.succeeding(saved -> ctx.verify(() -> {
          assertThat(saved).isNotNull();
          assertThat(saved.getEventId()).isEqualTo(1L);
          assertThat(saved.getTopic()).isEqualTo("card.payment.posted");
          assertThat(saved.getEventType()).isEqualTo("PAYMENT_POSTED");
          assertThat(saved.getCardNumber()).isEqualTo("4111111111111111");
          assertThat(saved.getReferenceId()).isEqualTo("ref-123");
          assertThat(saved.getPayload().getInteger("amount")).isEqualTo(100000);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("insert encodes null payload as empty JSONB object")
  void insertNullPayloadEncodedAsEmptyJson(VertxTestContext ctx) {
    mockPool();
    mockRow();
    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    CardEventLog eventLog = CardEventLog.builder()
        .topic("card.limit.changed")
        .eventType("LIMIT_CHANGED")
        .cardNumber("4111111111111111")
        .payload(null)
        .build();

    repo.insert(eventLog)
        .onComplete(ctx.succeeding(saved -> ctx.verify(() -> {
          assertThat(saved).isNotNull();
          ctx.completeNow();
        })));

    ArgumentCaptor<Tuple> captor = ArgumentCaptor.forClass(Tuple.class);
    verify(preparedQuery).execute(captor.capture());
    Tuple tuple = captor.getValue();
    assertThat(tuple.getString(0)).isEqualTo("card.limit.changed");
    assertThat(tuple.getString(1)).isEqualTo("LIMIT_CHANGED");
    assertThat(tuple.getString(4)).isEqualTo("{}");
  }
}
