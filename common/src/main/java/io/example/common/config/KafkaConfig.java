package io.example.common.config;

import io.vertx.core.Vertx;
import io.vertx.kafka.client.producer.KafkaProducer;
import java.util.HashMap;
import java.util.Map;

public class KafkaConfig {
  public static KafkaProducer<String, String> createProducer(Vertx vertx) {
    Map<String, String> config = new HashMap<>();
    config.put("bootstrap.servers", System.getenv().getOrDefault("KAFKA_BROKERS", "localhost:9092"));
    config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    config.put("acks", "1");
    
    return KafkaProducer.create(vertx, config);
  }
}
