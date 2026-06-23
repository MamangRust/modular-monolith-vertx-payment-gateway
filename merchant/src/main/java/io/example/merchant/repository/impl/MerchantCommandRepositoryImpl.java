package io.example.merchant.repository.impl;

import java.security.SecureRandom;
import io.example.merchant.model.Merchant;
import io.example.merchant.repository.MerchantCommandRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;
import pb.merchant.MerchantCommand.CreateMerchantRequest;
import pb.merchant.MerchantCommand.UpdateMerchantRequest;
import pb.merchant.MerchantCommand.UpdateMerchantStatusRequest;

@RequiredArgsConstructor
public class MerchantCommandRepositoryImpl implements MerchantCommandRepository {
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private final Pool pool;

  public static String generateApiKey() {
    byte[] key = new byte[32];
    SECURE_RANDOM.nextBytes(key);
    StringBuilder sb = new StringBuilder(key.length * 2);
    for (byte b : key) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  @Override
  public Future<Merchant> createMerchant(CreateMerchantRequest request) {
    String apiKey = generateApiKey();
    String sql = """
        INSERT INTO merchants (name, api_key, user_id, status, created_at, updated_at)
        VALUES ($1, $2, $3, 'pending', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING merchant_id AS id, name, api_key, user_id, status, created_at, updated_at, deleted_at
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(request.getName(), apiKey, request.getUserId()))
        .map(rows -> rows.iterator().hasNext() ? Merchant.fromRow(rows.iterator().next()) : null);
  }

  @Override
  public Future<Merchant> updateMerchant(UpdateMerchantRequest request) {
    String sql = """
        UPDATE merchants
        SET name = $2, user_id = $3, status = $4, updated_at = CURRENT_TIMESTAMP
        WHERE merchant_id = $1 AND deleted_at IS NULL
        RETURNING merchant_id AS id, name, api_key, user_id, status, created_at, updated_at, deleted_at
        """;
    return pool.preparedQuery(sql)
        .execute(Tuple.of(request.getMerchantId(), request.getName(), request.getUserId(), request.getStatus()))
        .map(rows -> rows.iterator().hasNext() ? Merchant.fromRow(rows.iterator().next()) : null);
  }

  @Override
  public Future<Merchant> updateMerchantStatus(UpdateMerchantStatusRequest request) {
    String sql = """
        UPDATE merchants
        SET status = $2, updated_at = CURRENT_TIMESTAMP
        WHERE merchant_id = $1 AND deleted_at IS NULL
        RETURNING merchant_id AS id, name, api_key, user_id, status, created_at, updated_at, deleted_at
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(request.getMerchantId(), request.getStatus()))
        .map(rows -> rows.iterator().hasNext() ? Merchant.fromRow(rows.iterator().next()) : null);
  }

  @Override
  public Future<Merchant> trashedMerchant(Integer merchantId) {
    String sql = """
        UPDATE merchants SET deleted_at = CURRENT_TIMESTAMP WHERE merchant_id = $1 AND deleted_at IS NULL
        RETURNING merchant_id AS id, name, api_key, user_id, status, created_at, updated_at, deleted_at
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(merchantId))
        .map(rows -> rows.iterator().hasNext() ? Merchant.fromRow(rows.iterator().next()) : null);
  }

  @Override
  public Future<Merchant> restoreMerchant(Integer merchantId) {
    String sql = """
        UPDATE merchants SET deleted_at = NULL WHERE merchant_id = $1 AND deleted_at IS NOT NULL
        RETURNING merchant_id AS id, name, api_key, user_id, status, created_at, updated_at, deleted_at
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(merchantId))
        .map(rows -> rows.iterator().hasNext() ? Merchant.fromRow(rows.iterator().next()) : null);
  }

  @Override
  public Future<Boolean> deleteMerchantPermanent(Integer merchantId) {
    return pool.preparedQuery("DELETE FROM merchants WHERE merchant_id = $1 AND deleted_at IS NOT NULL")
        .execute(Tuple.of(merchantId))
        .map(rows -> rows.rowCount() > 0);
  }

  @Override
  public Future<Integer> restoreAllMerchants() {
    return pool.query("UPDATE merchants SET deleted_at = NULL WHERE deleted_at IS NOT NULL")
        .execute()
        .map(rows -> rows.rowCount());
  }

  @Override
  public Future<Integer> deleteAllMerchantsPermanent() {
    return pool.query("DELETE FROM merchants WHERE deleted_at IS NOT NULL")
        .execute()
        .map(rows -> rows.rowCount());
  }
}
