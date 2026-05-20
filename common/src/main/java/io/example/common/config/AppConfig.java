package io.example.common.config;

import io.vertx.core.json.JsonObject;

public class AppConfig {

  private final JsonObject config;

  public AppConfig(JsonObject config) {
    this.config = config;
  }

  public String getHost() {
    return System.getenv().getOrDefault("APP_HOST", config.getString("host", "0.0.0.0"));
  }

  public int getPort() {
    String envPort = System.getenv("APP_PORT");
    if (envPort != null) return Integer.parseInt(envPort);
    return config.getInteger("port", 8080);
  }

  public int getGrpcPort() {
    String envPort = System.getenv("GRPC_PORT");
    if (envPort != null) return Integer.parseInt(envPort);
    return config.getInteger("grpc_port", 8083);
  }

  public JsonObject getDatabaseConfig() {
    JsonObject db = config.getJsonObject("database", new JsonObject());
    
    String host = System.getenv().getOrDefault("DB_HOST", db.getString("host", "localhost"));
    int port = System.getenv("DB_PORT") != null ? Integer.parseInt(System.getenv("DB_PORT")) : db.getInteger("port", 5432);
    String database = System.getenv().getOrDefault("DB_NAME", db.getString("database", "vertxdb"));
    String user = System.getenv().getOrDefault("DB_USERNAME", db.getString("user", "vertx"));
    String password = System.getenv().getOrDefault("DB_PASSWORD", db.getString("password", "vertx"));
    int poolSize = System.getenv("DB_MAX_OPEN_CONNS") != null ? Integer.parseInt(System.getenv("DB_MAX_OPEN_CONNS")) : db.getInteger("pool_size", 5);

    return new JsonObject()
      .put("host", host)
      .put("port", port)
      .put("database", database)
      .put("user", user)
      .put("password", password)
      .put("pool_size", poolSize);
  }

  public String getKafkaBrokers() {
    return System.getenv().getOrDefault("KAFKA_BROKERS", config.getString("kafka_brokers", "localhost:9092"));
  }

  public static AppConfig from(JsonObject config) {
    return new AppConfig(config);
  }
}
