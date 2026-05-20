package io.example.common.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaService {
  private static final Logger log = LoggerFactory.getLogger(KafkaService.class);
  private final KafkaProducer<String, String> producer;

  public KafkaService(KafkaProducer<String, String> producer) {
    this.producer = producer;
  }

  public Future<Void> sendMessage(String topic, String key, JsonObject value) {
    KafkaProducerRecord<String, String> record = KafkaProducerRecord.create(topic, key, value.encode());
    return producer.send(record)
        .onSuccess(metadata -> log.info("📤 Message sent to topic {}: {}", topic, value.encode()))
        .onFailure(err -> log.error("❌ Failed to send message to topic {}", topic, err))
        .mapEmpty();
  }

  public void close() {
    if (producer != null) {
      producer.close();
    }
  }
}
