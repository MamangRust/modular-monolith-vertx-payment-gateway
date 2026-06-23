package io.example.merchant.repository.impl;

import java.util.ArrayList;
import java.util.List;

import io.example.common.domain.PagedResult;
import io.example.merchant.model.MerchantTransactions;
import io.example.merchant.repository.MerchantTransactionRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;
import pb.merchant.Merchant.FindAllMerchantTransaction;
import pb.merchant.Merchant.FindAllMerchantTransactionApikey;
import pb.merchant.Merchant.FindAllMerchantTransactionId;

@RequiredArgsConstructor
public class MerchantTransactionRepositoryImpl implements MerchantTransactionRepository {
  private final Pool pool;

  private String normalizeSearch(String search) {
    return (search == null || search.isBlank()) ? null : search;
  }

  @Override
  public Future<PagedResult<MerchantTransactions>> findAllTransactionMerchant(FindAllMerchantTransaction request) {
    int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 10;
    int offset = Math.max(0, request.getPage() - 1) * pageSize;
    String search = normalizeSearch(request.getSearch());

    String sql = """
        SELECT
            t.transaction_id,
            t.card_number,
            t.amount,
            t.payment_method,
            t.merchant_id,
            m.name AS merchant_name,
            t.transaction_time,
            t.created_at,
            t.updated_at,
            t.deleted_at,
            COUNT(*) OVER () AS total_count
        FROM transactions t
            JOIN merchants m ON t.merchant_id = m.merchant_id
        WHERE
            t.deleted_at IS NULL
            AND (
                $1::TEXT IS NULL
                OR t.card_number ILIKE '%' || $1 || '%'
                OR t.payment_method ILIKE '%' || $1 || '%'
            )
        ORDER BY t.transaction_time DESC
        LIMIT $2
        OFFSET
            $3;
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(search, pageSize, offset))
        .map(rows -> {
          List<MerchantTransactions> list = new ArrayList<>();
          int total = 0;
          for (Row row : rows) {
            list.add(MerchantTransactions.fromRow(row));
            if (total == 0) {
              Integer tc = row.getInteger("total_count");
              if (tc != null)
                total = tc;
            }
          }
          return new PagedResult<>(list, total);
        });
  }

  @Override
  public Future<PagedResult<MerchantTransactions>> findAllTransactionByMerchant(FindAllMerchantTransactionId request) {
    int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 10;
    int offset = Math.max(0, request.getPage() - 1) * pageSize;
    String search = normalizeSearch(request.getSearch());

    String sql = """
        SELECT
            t.transaction_id,
            t.card_number,
            t.amount,
            t.payment_method,
            t.merchant_id,
            m.name AS merchant_name,
            t.transaction_time,
            t.created_at,
            t.updated_at,
            t.deleted_at,
            COUNT(*) OVER () AS total_count
        FROM transactions t
            JOIN merchants m ON t.merchant_id = m.merchant_id
        WHERE
            t.deleted_at IS NULL
            AND t.merchant_id = $1
            AND (
                $2::TEXT IS NULL
                OR t.card_number ILIKE '%' || $2 || '%'
                OR t.payment_method ILIKE '%' || $2 || '%'
            )
        ORDER BY t.transaction_time DESC
        LIMIT $3
        OFFSET
            $4;
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(request.getId(), search, pageSize, offset))
        .map(rows -> {
          List<MerchantTransactions> list = new ArrayList<>();
          int total = 0;
          for (Row row : rows) {
            list.add(MerchantTransactions.fromRow(row));
            if (total == 0) {
              Integer tc = row.getInteger("total_count");
              if (tc != null)
                total = tc;
            }
          }
          return new PagedResult<>(list, total);
        });
  }

  @Override
  public Future<PagedResult<MerchantTransactions>> findAllTransactionByApikey(
      FindAllMerchantTransactionApikey request) {
    int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 10;
    int offset = Math.max(0, request.getPage() - 1) * pageSize;
    String search = normalizeSearch(request.getSearch());

    String sql = """
        SELECT
            t.transaction_id,
            t.card_number,
            t.amount,
            t.payment_method,
            t.merchant_id,
            m.name AS merchant_name,
            t.transaction_time,
            t.created_at,
            t.updated_at,
            t.deleted_at,
            COUNT(*) OVER () AS total_count
        FROM transactions t
            JOIN merchants m ON t.merchant_id = m.merchant_id
        WHERE
            t.deleted_at IS NULL
            AND m.api_key = $1
            AND (
                $2::TEXT IS NULL
                OR t.card_number ILIKE '%' || $2 || '%'
                OR t.payment_method ILIKE '%' || $2 || '%'
            )
        ORDER BY t.transaction_time DESC
        LIMIT $3
        OFFSET
            $4;
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(request.getApiKey(), search, pageSize, offset))
        .map(rows -> {
          List<MerchantTransactions> list = new ArrayList<>();
          int total = 0;
          for (Row row : rows) {
            list.add(MerchantTransactions.fromRow(row));
            if (total == 0) {
              Integer tc = row.getInteger("total_count");
              if (tc != null)
                total = tc;
            }
          }
          return new PagedResult<>(list, total);
        });
  }
}
