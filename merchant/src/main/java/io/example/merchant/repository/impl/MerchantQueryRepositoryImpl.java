package io.example.merchant.repository.impl;

import java.util.ArrayList;
import java.util.List;

import io.example.common.domain.PagedResult;
import io.example.merchant.model.Merchant;
import io.example.merchant.repository.MerchantQueryRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import pb.merchant.Merchant.FindAllMerchantRequest;

public class MerchantQueryRepositoryImpl implements MerchantQueryRepository {
  private final Pool pool;

  public MerchantQueryRepositoryImpl(Pool pool) {
    this.pool = pool;
  }

  private String normalizeSearch(String search) {
    return (search == null || search.isBlank()) ? null : search;
  }

  @Override
  public Future<PagedResult<Merchant>> findAllMerchants(FindAllMerchantRequest request) {
    int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 10;
    int offset = Math.max(0, request.getPage() - 1) * pageSize;
    String search = normalizeSearch(request.getSearch());

    String sql = """
        SELECT merchant_id AS id, name, api_key, user_id, status, created_at, updated_at, deleted_at, COUNT(*) OVER() AS total_count
        FROM merchants
        WHERE ($1::TEXT IS NULL OR name ILIKE '%' || $1 || '%' OR api_key ILIKE '%' || $1 || '%' OR status ILIKE '%' || $1 || '%')
        ORDER BY merchant_id LIMIT $2 OFFSET $3
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(search, pageSize, offset))
        .map(rows -> {
          List<Merchant> list = new ArrayList<>();
          int total = 0;
          for (Row row : rows) {
            list.add(Merchant.fromRow(row));
            if (total == 0)
              total = row.getInteger("total_count");
          }
          return new PagedResult<>(list, total);
        });
  }

  @Override
  public Future<PagedResult<Merchant>> findByActive(FindAllMerchantRequest request) {
    int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 10;
    int offset = Math.max(0, request.getPage() - 1) * pageSize;
    String search = normalizeSearch(request.getSearch());

    String sql = """
        SELECT merchant_id AS id, name, api_key, user_id, status, created_at, updated_at, deleted_at, COUNT(*) OVER() AS total_count
        FROM merchants
        WHERE deleted_at IS NULL
          AND ($1::TEXT IS NULL OR name ILIKE '%' || $1 || '%' OR api_key ILIKE '%' || $1 || '%' OR status ILIKE '%' || $1 || '%')
        ORDER BY merchant_id LIMIT $2 OFFSET $3
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(search, pageSize, offset))
        .map(rows -> {
          List<Merchant> list = new ArrayList<>();
          int total = 0;
          for (Row row : rows) {
            list.add(Merchant.fromRow(row));
            if (total == 0)
              total = row.getInteger("total_count");
          }
          return new PagedResult<>(list, total);
        });
  }

  @Override
  public Future<PagedResult<Merchant>> findByTrashed(FindAllMerchantRequest request) {
    int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 10;
    int offset = Math.max(0, request.getPage() - 1) * pageSize;
    String search = normalizeSearch(request.getSearch());

    String sql = """
        SELECT merchant_id AS id, name, api_key, user_id, status, created_at, updated_at, deleted_at, COUNT(*) OVER() AS total_count
        FROM merchants
        WHERE deleted_at IS NOT NULL
          AND ($1::TEXT IS NULL OR name ILIKE '%' || $1 || '%' OR api_key ILIKE '%' || $1 || '%' OR status ILIKE '%' || $1 || '%')
        ORDER BY merchant_id LIMIT $2 OFFSET $3
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(search, pageSize, offset))
        .map(rows -> {
          List<Merchant> list = new ArrayList<>();
          int total = 0;
          for (Row row : rows) {
            list.add(Merchant.fromRow(row));
            if (total == 0)
              total = row.getInteger("total_count");
          }
          return new PagedResult<>(list, total);
        });
  }

  @Override
  public Future<Merchant> findByApiKey(String apiKey) {
    String sql = "SELECT merchant_id AS id, name, api_key, user_id, status, created_at, updated_at, deleted_at FROM merchants WHERE api_key = $1 AND deleted_at IS NULL";
    return pool.preparedQuery(sql).execute(Tuple.of(apiKey))
        .map(rows -> rows.iterator().hasNext() ? Merchant.fromRow(rows.iterator().next()) : null);
  }

  @Override
  public Future<Merchant> findByMerchantId(int merchantId) {
    String sql = "SELECT merchant_id AS id, name, api_key, user_id, status, created_at, updated_at, deleted_at FROM merchants WHERE merchant_id = $1 AND deleted_at IS NULL";
    return pool.preparedQuery(sql).execute(Tuple.of(merchantId))
        .map(rows -> rows.iterator().hasNext() ? Merchant.fromRow(rows.iterator().next()) : null);
  }

  @Override
  public Future<Merchant> findByName(String name) {
    String sql = "SELECT merchant_id AS id, name, api_key, user_id, status, created_at, updated_at, deleted_at FROM merchants WHERE name = $1 AND deleted_at IS NULL";
    return pool.preparedQuery(sql).execute(Tuple.of(name))
        .map(rows -> rows.iterator().hasNext() ? Merchant.fromRow(rows.iterator().next()) : null);
  }

  @Override
  public Future<List<Merchant>> findByMerchantUserId(int userId) {
    String sql = "SELECT merchant_id AS id, name, api_key, user_id, status, created_at, updated_at, deleted_at FROM merchants WHERE user_id = $1 AND deleted_at IS NULL";
    return pool.preparedQuery(sql).execute(Tuple.of(userId))
        .map(rows -> {
          List<Merchant> list = new ArrayList<>();
          for (Row r : rows)
            list.add(Merchant.fromRow(r));
          return list;
        });
  }
}
