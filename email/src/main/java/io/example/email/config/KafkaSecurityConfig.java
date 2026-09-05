package io.example.email.config;

import java.util.Map;

/**
 * Applies the Kafka security configuration to a producer/consumer config map
 * (Phase 5 baseline). Fully environment-driven so the same image runs plaintext
 * in dev and SASL/TLS in production:
 *
 * <ul>
 *   <li>{@code KAFKA_SECURITY_PROTOCOL} — {@code PLAINTEXT} (default, dev),
 *       {@code SASL_PLAINTEXT}, {@code SSL}, {@code SASL_SSL};</li>
 *   <li>{@code KAFKA_SASL_MECHANISM} — default {@code SCRAM-SHA-256};</li>
 *   <li>{@code KAFKA_SASL_JAAS_CONFIG} — full JAAS string; otherwise built from
 *       {@code KAFKA_SASL_USERNAME} + {@code KAFKA_SASL_PASSWORD};</li>
 *   <li>{@code KAFKA_SSL_TRUSTSTORE_LOCATION/PASSWORD}, {@code KAFKA_SSL_KEYSTORE_LOCATION/PASSWORD},
 *       {@code KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM} (default {@code https}).</li>
 * </ul>
 *
 * <p>Credentials are only ever written into the Kafka client config (never logged).
 */
public final class KafkaSecurityConfig {

  private KafkaSecurityConfig() {
  }

  /** Reads the security settings from the process environment. */
  public static void apply(Map<String, String> config) {
    apply(config, System.getenv());
  }

  /** Testable variant: reads the security settings from {@code env}. */
  public static void apply(Map<String, String> config, Map<String, String> env) {
    String protocol = upper(env.getOrDefault("KAFKA_SECURITY_PROTOCOL", "PLAINTEXT"));
    if (protocol.isBlank() || "PLAINTEXT".equals(protocol)) {
      return;
    }
    config.put("security.protocol", protocol);

    if (protocol.contains("SASL")) {
      config.put("sasl.mechanism", defaulted(env.get("KAFKA_SASL_MECHANISM"), "SCRAM-SHA-256"));
      String jaas = defaulted(env.get("KAFKA_SASL_JAAS_CONFIG"), null);
      if (jaas == null) {
        String user = env.get("KAFKA_SASL_USERNAME");
        String password = env.get("KAFKA_SASL_PASSWORD");
        if (user != null && !user.isBlank() && password != null && !password.isBlank()) {
          jaas = "org.apache.kafka.common.security.scram.ScramLoginModule required "
              + "username=\"" + user + "\" password=\"" + password + "\";";
        }
      }
      if (jaas != null) {
        config.put("sasl.jaas.config", jaas);
      }
    }

    if (protocol.contains("SSL")) {
      putIfPresent(config, env, "ssl.truststore.location", "KAFKA_SSL_TRUSTSTORE_LOCATION");
      putIfPresent(config, env, "ssl.truststore.password", "KAFKA_SSL_TRUSTSTORE_PASSWORD");
      putIfPresent(config, env, "ssl.keystore.location", "KAFKA_SSL_KEYSTORE_LOCATION");
      putIfPresent(config, env, "ssl.keystore.password", "KAFKA_SSL_KEYSTORE_PASSWORD");
      config.put("ssl.endpoint.identification.algorithm",
          defaulted(env.get("KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM"), "https"));
    }
  }

  private static void putIfPresent(Map<String, String> config, Map<String, String> env,
                                   String kafkaKey, String envKey) {
    String value = env.get(envKey);
    if (value != null && !value.isBlank()) {
      config.put(kafkaKey, value);
    }
  }

  private static String upper(String value) {
    return value == null ? "" : value.trim().toUpperCase();
  }

  private static String defaulted(String value, String fallback) {
    return (value == null || value.isBlank()) ? fallback : value.trim();
  }
}
