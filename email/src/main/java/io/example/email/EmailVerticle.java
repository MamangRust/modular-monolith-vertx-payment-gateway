package io.example.email;

import io.example.common.config.TelemetryConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmailVerticle extends AbstractVerticle {
  private static final Logger log = LoggerFactory.getLogger(EmailVerticle.class);

  private MailClient mailClient;
  private KafkaConsumer<String, JsonObject> consumer;
  private TelemetryConfig telemetryConfig;

  @Override
  public void start(Promise<Void> startPromise) {
    JsonObject config = config();
    
    // 1. Setup Telemetry
    telemetryConfig = new TelemetryConfig(config);
    telemetryConfig.initialize();

    // 2. Setup Mail Client
    MailConfig mailConfig = new MailConfig()
        .setHostname(System.getenv().getOrDefault("SMTP_SERVER", "localhost"))
        .setPort(Integer.parseInt(System.getenv().getOrDefault("SMTP_PORT", "587")))
        .setUsername(System.getenv("SMTP_USER"))
        .setPassword(System.getenv("SMTP_PASS"))
        .setStarttls(io.vertx.ext.mail.StartTLSOptions.REQUIRED);
    
    mailClient = MailClient.createShared(vertx, mailConfig);

    // 3. Setup Kafka Consumer
    Map<String, String> kafkaConfig = new HashMap<>();
    kafkaConfig.put("bootstrap.servers", System.getenv().getOrDefault("KAFKA_BROKERS", "localhost:9092"));
    kafkaConfig.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    kafkaConfig.put("value.deserializer", "io.vertx.kafka.client.serialization.JsonObjectDeserializer");
    kafkaConfig.put("group.id", "email-service-group");
    kafkaConfig.put("auto.offset.reset", "earliest");

    consumer = KafkaConsumer.create(vertx, kafkaConfig);

    // List of topics to subscribe to
    List<String> topics = Arrays.asList(
        "email-service-topic-auth-register",
        "email-service-topic-auth-forgot-password",
        "email-service-topic-auth-verify-code-success",
        "email-service-topic-saldo-create",
        "email-service-topic-topup-create",
        "email-service-topic-transaction-create",
        "email-service-topic-transfer-create",
        "email-service-topic-merchant-create",
        "email-service-topic-merchant-update-status",
        "email-service-topic-merchant-document-create",
        "email-service-topic-merchant-document-update-status"
    );

    consumer.handler(record -> {
      JsonObject emailReq = record.value();
      log.info("📥 Received message from topic {}: {}", record.topic(), emailReq.encode());
      sendEmail(emailReq);
    });

    consumer.subscribe(new java.util.HashSet<>(topics))
        .onSuccess(v -> {
          log.info("📧 Email Service successfully started and subscribed to {} topics", topics.size());
          startPromise.complete();
        })
        .onFailure(err -> {
          log.error("❌ Failed to start Email Service subscription", err);
          startPromise.fail(err);
        });
  }

  private void sendEmail(JsonObject payload) {
    try {
      // Logic follows the provided example: payload contains email, subject, body
      String email = payload.getString("email");
      String subject = payload.getString("subject");
      String body = payload.getString("body");

      if (email == null || subject == null || body == null) {
        log.warn("⚠️ Received incomplete email payload: {}", payload.encode());
        return;
      }

      MailMessage message = new MailMessage()
          .setFrom("no-reply@payment-gateway.com")
          .setTo(email)
          .setSubject(subject)
          .setHtml(body); // Using setHtml as most notification bodies are HTML

      mailClient.sendMail(message)
          .onSuccess(result -> log.info("✅ Email successfully sent to {}", email))
          .onFailure(err -> log.error("❌ Failed to send email to {}", email, err));
          
    } catch (Exception e) {
      log.error("❌ Error processing email record", e);
    }
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    if (consumer != null) consumer.close();
    if (telemetryConfig != null) telemetryConfig.shutdown();
    stopPromise.complete();
  }
}
