package io.example.migration;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigrationApp {
  private static final Logger log = LoggerFactory.getLogger(MigrationApp.class);

  public static void main(String[] args) {
    log.info("🚀 Starting database migration runner...");

    String host = System.getenv().getOrDefault("DB_HOST_MIGRATE", 
                  System.getenv().getOrDefault("DB_HOST", "localhost"));
    String portStr = System.getenv().getOrDefault("DB_PORT", "5432");
    String dbName = System.getenv().getOrDefault("DB_NAME", "PAYMENT_GATEWAY");
    String username = System.getenv().getOrDefault("DB_USERNAME", "DRAGON");
    String password = System.getenv().getOrDefault("DB_PASSWORD", "DRAGON");

    String url = String.format("jdbc:postgresql://%s:%s/%s", host, portStr, dbName);

    log.info("Connecting to database for migration at: jdbc:postgresql://{}:{}/{}", host, portStr, dbName);

    Flyway flyway = Flyway.configure()
        .dataSource(url, username, password)
        .baselineOnMigrate(true)
        .load();

    try {
      flyway.migrate();
      log.info("✅ Database migration completed successfully!");
      System.exit(0);
    } catch (Exception e) {
      log.error("❌ Database migration failed!", e);
      System.exit(1);
    }
  }
}
