package io.example.common.config;

import java.util.Arrays;

import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisOptions;

public class RedisConfig {

  public static RedisAPI createClient(Vertx vertx) {
    String redisHost = System.getenv().getOrDefault("REDIS_HOST", "localhost");
    int redisPort = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
    String redisPassword = System.getenv().getOrDefault("REDIS_PASSWORD", "dragon_knight");
    boolean clusterEnabled = Boolean.parseBoolean(System.getenv().getOrDefault("REDIS_CLUSTER_ENABLED", "false"));

    RedisOptions options = new RedisOptions()
        .setPassword(redisPassword);

    if (clusterEnabled) {
      options.setType(RedisClientType.CLUSTER);
      String endpoints = System.getenv().getOrDefault("REDIS_CLUSTER_ENDPOINTS", redisHost + ":" + redisPort);
      Arrays.stream(endpoints.split(","))
          .map(endpoint -> endpoint.trim().startsWith("redis://") ? endpoint.trim() : "redis://" + endpoint.trim())
          .forEach(options::addConnectionString);
    } else {
      options.setType(RedisClientType.STANDALONE)
          .setConnectionString("redis://" + redisHost + ":" + redisPort);
    }

    return RedisAPI.api(Redis.createClient(vertx, options));
  }
}
