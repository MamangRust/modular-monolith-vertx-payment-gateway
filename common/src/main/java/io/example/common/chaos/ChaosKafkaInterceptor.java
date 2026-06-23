package io.example.common.chaos;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import io.vertx.kafka.client.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * JDK dynamic proxy that wraps a {@link KafkaProducer} and intercepts
 * {@code send(KafkaProducerRecord)} calls to inject chaos:
 * <ul>
 *   <li>{@code dropMessage} — silently drop the message (return succeeded future)</li>
 *   <li>{@code rejectMessage} — fail the future with an error</li>
 *   <li>{@code latencyMs} — delay the send by the configured amount</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * KafkaProducer<String, String> raw = KafkaProducer.create(vertx, config);
 * KafkaProducer<String, String> chaos = ChaosKafkaInterceptor.wrap(raw, chaosManager, vertx);
 * }</pre>
 */
public class ChaosKafkaInterceptor implements InvocationHandler {
  private static final Logger log = LoggerFactory.getLogger(ChaosKafkaInterceptor.class);

  private final Object delegate;
  private final ChaosManager manager;
  private final Vertx vertx;

  public ChaosKafkaInterceptor(Object delegate, ChaosManager manager, Vertx vertx) {
    this.delegate = delegate;
    this.manager = manager;
    this.vertx = vertx;
  }

  /**
   * Wrap a {@link KafkaProducer} with chaos interception.
   */
  @SuppressWarnings("unchecked")
  public static <K, V> KafkaProducer<K, V> wrap(KafkaProducer<K, V> producer, ChaosManager manager, Vertx vertx) {
    return (KafkaProducer<K, V>) Proxy.newProxyInstance(
        KafkaProducer.class.getClassLoader(),
        new Class<?>[]{KafkaProducer.class},
        new ChaosKafkaInterceptor(producer, manager, vertx));
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String methodName = method.getName();

    // Intercept producer.send(record) — the single-message send variant
    if ("send".equals(methodName) && args != null && args.length == 1 && args[0] instanceof KafkaProducerRecord) {
      KafkaProducerRecord<?, ?> record = (KafkaProducerRecord<?, ?>) args[0];
      String topic = record.topic();

      ChaosPolicy policy = manager.evaluate("kafka", topic);
      // Fallback: match by topic suffix/prefix if exact match fails
      if (policy == null) {
        policy = findTopicMatch(topic);
      }

      if (policy != null && policy.isEnabled() && Math.random() < policy.getErrorChance()) {
        log.info("🔥 Injecting Kafka chaos [Policy: {}] to topic: {}", policy.getName(), topic);

        if (policy.isDropMessage()) {
          log.warn("💣 Dropping Kafka message to topic: {} (simulated loss)", topic);
          RecordMetadata fakeMetadata = new RecordMetadata();
          fakeMetadata.setTopic(topic);
          return Future.succeededFuture(fakeMetadata);
        }

        if (policy.isRejectMessage()) {
          log.warn("💥 Rejecting Kafka message to topic: {} (simulated failure)", topic);
          String msg = policy.getErrorMessage() != null
              ? policy.getErrorMessage()
              : "Simulated Kafka message rejection by chaos";
          return Future.failedFuture(new RuntimeException(msg));
        }

        long latency = policy.getLatencyMs();
        if (latency > 0) {
          log.warn("⏳ Delaying Kafka message to topic: {} by {} ms", topic, latency);
          Promise<RecordMetadata> promise = Promise.promise();
          vertx.setTimer(latency, id -> {
            try {
              Future<RecordMetadata> real = (Future<RecordMetadata>) method.invoke(delegate, args);
              real.onComplete(ar -> {
                if (ar.succeeded()) {
                  promise.complete(ar.result());
                } else {
                  promise.fail(ar.cause());
                }
              });
            } catch (Exception e) {
              promise.fail(e.getCause() != null ? e.getCause() : e);
            }
          });
          return promise.future();
        }
      }
    }

    // All other methods (close(), partitionsFor(), flush(), etc.) pass through
    try {
      return method.invoke(delegate, args);
    } catch (Exception e) {
      throw e.getCause() != null ? e.getCause() : e;
    }
  }

  private ChaosPolicy findTopicMatch(String topic) {
    for (ChaosPolicy policy : manager.getPolicies()) {
      if (policy.isEnabled() && "kafka".equalsIgnoreCase(policy.getType())) {
        String target = policy.getTarget();
        if (topic != null && target != null && topic.contains(target)) {
          return policy;
        }
      }
    }
    return null;
  }
}
