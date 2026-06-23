package io.example.transaction.repository.impl;

import java.util.ArrayList;
import java.util.List;

import io.example.common.domain.PagedResult;
import io.example.transaction.domain.requests.FindAllTransactionCardNumber;
import io.example.transaction.domain.requests.FindAllTransactions;
import io.example.transaction.model.Transaction;
import io.example.transaction.repository.TransactionQueryRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TransactionQueryRepositoryImpl implements TransactionQueryRepository {
  private final Pool pool;

  private String normalizeSearch(String search) {
    return (search == null || search.isBlank()) ? null : search;
  }

  @Override
  public Future<PagedResult<Transaction>> getTransactions(FindAllTransactions req) {
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    int offset = Math.max(0, req.getPage() - 1) * pageSize;
    String sql = """
        SELECT *, COUNT(*) OVER() AS total_count FROM transactions
        WHERE deleted_at IS NULL
          AND ($1::TEXT IS NULL OR card_number ILIKE '%' || $1 || '%' OR payment_method ILIKE '%' || $1 || '%' OR status ILIKE '%' || $1 || '%')
        ORDER BY transaction_time DESC LIMIT $2 OFFSET $3
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(normalizeSearch(req.getSearch()), pageSize, offset))
        .map(this::mapPaged);
  }

  @Override
  public Future<PagedResult<Transaction>> getActiveTransactions(FindAllTransactions req) {
    return getTransactions(req);
  }

  @Override
  public Future<PagedResult<Transaction>> getTrashedTransactions(FindAllTransactions req) {
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    int offset = Math.max(0, req.getPage() - 1) * pageSize;
    String sql = """
        SELECT *, COUNT(*) OVER() AS total_count FROM transactions
        WHERE deleted_at IS NOT NULL
          AND ($1::TEXT IS NULL OR card_number ILIKE '%' || $1 || '%' OR status ILIKE '%' || $1 || '%')
        ORDER BY transaction_time DESC LIMIT $2 OFFSET $3
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(normalizeSearch(req.getSearch()), pageSize, offset))
        .map(this::mapPaged);
  }

  @Override
  public Future<Transaction> getTransactionById(Integer transactionId) {
    String sql = "SELECT * FROM transactions WHERE transaction_id = $1 AND deleted_at IS NULL";
    return pool.preparedQuery(sql).execute(Tuple.of(transactionId)).map(this::mapSingle);
  }

  @Override
  public Future<PagedResult<Transaction>> getTransactionsByCardNumber(FindAllTransactionCardNumber req) {
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    int offset = Math.max(0, req.getPage() - 1) * pageSize;
    String sql = """
        SELECT *, COUNT(*) OVER () AS total_count
        FROM transactions
        WHERE deleted_at IS NULL
          AND card_number = $1
          AND ($2::TEXT IS NULL OR payment_method ILIKE '%' || $2 || '%')
        ORDER BY transaction_time DESC
        LIMIT $3 OFFSET $4
        """;
    return pool.preparedQuery(sql)
        .execute(Tuple.of(req.getCardNumber(), normalizeSearch(req.getSearch()), pageSize, offset))
        .map(this::mapPaged);
  }

  @Override
  public Future<PagedResult<Transaction>> getTransactionsByMerchantId(Integer merchantId) {
    String sql = """
        SELECT *, COUNT(*) OVER () AS total_count
        FROM transactions
        WHERE deleted_at IS NULL AND merchant_id = $1
        ORDER BY transaction_time DESC
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(merchantId))
        .map(this::mapPaged);
  }

  @Override
  public Future<Transaction> findByTrashed(Integer transactionId) {
    String sql = "SELECT * FROM transactions WHERE transaction_id = $1 AND deleted_at IS NOT NULL";
    return pool.preparedQuery(sql).execute(Tuple.of(transactionId)).map(this::mapSingle);
  }

  private Transaction mapSingle(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? Transaction.fromRow(rows.iterator().next()) : null;
  }

  private PagedResult<Transaction> mapPaged(RowSet<Row> rows) {
    List<Transaction> list = new ArrayList<>();
    int total = 0;
    for (Row row : rows) {
      list.add(Transaction.fromRow(row));
      if (total == 0) {
        Object tcVal = row.getValue("total_count");
        if (tcVal instanceof Number num) {
          total = num.intValue();
        }
      }
    }
    return new PagedResult<>(list, total);
  }
}
