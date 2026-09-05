package io.example.email.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class KafkaSecurityConfigTest {

  @Test
  void plaintextDefault_isNoOp() {
    Map<String, String> config = new HashMap<>();
    config.put("bootstrap.servers", "localhost:9092");

    KafkaSecurityConfig.apply(config, Map.of());

    assertThat(config).containsEntry("bootstrap.servers", "localhost:9092");
    assertThat(config).doesNotContainKey("security.protocol");
    assertThat(config).doesNotContainKey("sasl.mechanism");
    assertThat(config).doesNotContainKey("sasl.jaas.config");
    assertThat(config).doesNotContainKey("ssl.truststore.location");
  }

  @Test
  void saslSsl_appliesProtocolMechanismJaasAndTruststore() {
    Map<String, String> config = new HashMap<>();

    KafkaSecurityConfig.apply(config, Map.of(
        "KAFKA_SECURITY_PROTOCOL", "SASL_SSL",
        "KAFKA_SASL_MECHANISM", "SCRAM-SHA-512",
        "KAFKA_SASL_USERNAME", "svc-user",
        "KAFKA_SASL_PASSWORD", "s3cret",
        "KAFKA_SSL_TRUSTSTORE_LOCATION", "/certs/truststore.jks",
        "KAFKA_SSL_TRUSTSTORE_PASSWORD", "changeit"));

    assertThat(config.get("security.protocol")).isEqualTo("SASL_SSL");
    assertThat(config.get("sasl.mechanism")).isEqualTo("SCRAM-SHA-512");
    assertThat(config.get("sasl.jaas.config"))
        .contains("ScramLoginModule").contains("svc-user").contains("s3cret");
    assertThat(config.get("ssl.truststore.location")).isEqualTo("/certs/truststore.jks");
    assertThat(config.get("ssl.truststore.password")).isEqualTo("changeit");
    assertThat(config.get("ssl.endpoint.identification.algorithm")).isEqualTo("https");
  }

  @Test
  void saslPlaintext_usesDefaultMechanismAndBuiltJaas() {
    Map<String, String> config = new HashMap<>();

    KafkaSecurityConfig.apply(config, Map.of(
        "KAFKA_SECURITY_PROTOCOL", "SASL_PLAINTEXT",
        "KAFKA_SASL_USERNAME", "user1",
        "KAFKA_SASL_PASSWORD", "pass1"));

    assertThat(config.get("security.protocol")).isEqualTo("SASL_PLAINTEXT");
    assertThat(config.get("sasl.mechanism")).isEqualTo("SCRAM-SHA-256");
    assertThat(config.get("sasl.jaas.config")).contains("user1").contains("pass1");
    assertThat(config).doesNotContainKey("ssl.truststore.location");
  }

  @Test
  void explicitJaasConfig_winsOverUsernamePassword() {
    Map<String, String> config = new HashMap<>();

    KafkaSecurityConfig.apply(config, Map.of(
        "KAFKA_SECURITY_PROTOCOL", "SASL_SSL",
        "KAFKA_SASL_JAAS_CONFIG",
            "org.apache.kafka.common.security.plain.PlainLoginModule required;",
        "KAFKA_SASL_USERNAME", "ignored",
        "KAFKA_SASL_PASSWORD", "ignored"));

    assertThat(config.get("sasl.jaas.config")).contains("PlainLoginModule");
    assertThat(config.get("sasl.jaas.config")).doesNotContain("ignored");
  }
}
