package io.example.common.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import java.util.Objects;
import io.opentelemetry.context.propagation.TextMapPropagator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;

public class TracingMetrics {
  private final Tracer tracer;
  private final Meter meter;
  private final LongCounter requestCounter;
  private final DoubleHistogram requestDurationHistogram;
  private final TextMapPropagator propagator;

  private static final AttributeKey<String> METHOD_KEY = AttributeKey.stringKey("method");
  private static final AttributeKey<String> STATUS_KEY = AttributeKey.stringKey("status");

  public TracingMetrics(OpenTelemetry openTelemetry, String instrumentationName) {
    this.tracer = openTelemetry.getTracer(Objects.requireNonNull(instrumentationName));
    this.meter = openTelemetry.getMeter(Objects.requireNonNull(instrumentationName));
    this.propagator = openTelemetry.getPropagators().getTextMapPropagator();

    this.requestCounter = meter.counterBuilder("requests_total")
        .setDescription("Total number of requests")
        .build();

    this.requestDurationHistogram = meter.histogramBuilder("request_duration_seconds")
        .setDescription("Request duration in seconds")
        .setUnit("s")
        .build();
  }

  public Tracer getTracer() {
    return tracer;
  }

  private static final TextMapGetter<Map<String, String>> MAP_GETTER = new TextMapGetter<>() {
    @Override
    public Iterable<String> keys(@Nonnull Map<String, String> carrier) {
      return carrier.keySet();
    }

    @Override
    @Nullable
    public String get(@Nullable Map<String, String> carrier, @Nonnull String key) {
      return carrier != null ? carrier.get(key) : null;
    }
  };

  public void injectContext(@Nonnull Context context, @Nonnull Map<String, String> carrier) {
    propagator.inject(Objects.requireNonNull(context), Objects.requireNonNull(carrier), Map::put);
  }

  @Nonnull
  public Context extractContext(@Nonnull Map<String, String> carrier) {
    return Objects.requireNonNull(propagator.extract(
        Objects.requireNonNull(Context.current()),
        Objects.requireNonNull(carrier),
        Objects.requireNonNull(MAP_GETTER)));
  }

  public TracingContext startSpan(String operationName) {
    return startSpan(operationName, Attributes.empty());
  }

  public TracingContext startSpan(String operationName, Attributes attributes) {
    Instant startTime = Instant.now();

    Span span = tracer.spanBuilder(Objects.requireNonNull(operationName))
        .setSpanKind(SpanKind.SERVER)
        .setAllAttributes(Objects.requireNonNull(attributes))
        .startSpan();

    span.addEvent("Operation started", Objects.requireNonNull(Attributes.builder()
        .put("operation", Objects.requireNonNull(operationName))
        .put("timestamp", Objects.requireNonNull(startTime.toString()))
        .build()));

    Context context = Context.current().with(span);

    return new TracingContext(context, startTime);
  }

  public <T> T traceAndMeasure(String operationName, String method, Supplier<T> supplier) {
    return traceAndMeasure(operationName, method, Attributes.empty(), supplier);
  }

  public <T> T traceAndMeasure(String operationName, String method, Attributes attributes, Supplier<T> supplier) {
    TracingContext tracingContext = startSpan(operationName, attributes);

    try (Scope scope = tracingContext.getContext().makeCurrent()) {
      T result = supplier.get();
      completeSpanSuccess(tracingContext, method, "Operation completed successfully");
      return result;
    } catch (Exception e) {
      completeSpanError(tracingContext, method, "Operation failed: " + e.getMessage());
      throw e;
    }
  }

  public void completeSpanSuccess(TracingContext tracingContext, String method, String message) {
    completeSpan(tracingContext, method, true, message);
  }

  public void completeSpanError(TracingContext tracingContext, String method, String errorMessage) {
    completeSpan(tracingContext, method, false, errorMessage);
  }

  private void completeSpan(TracingContext tracingContext, String method, boolean isSuccess, String message) {
    String status = isSuccess ? "SUCCESS" : "ERROR";
    double duration = Duration.between(tracingContext.getStartTime(), Instant.now()).toMillis() / 1000.0;

    Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

    span.addEvent("Operation completed", Objects.requireNonNull(Attributes.builder()
        .put("status", Objects.requireNonNull(status))
        .put("duration_secs", duration)
        .put("message", Objects.requireNonNull(message))
        .build()));

    if (isSuccess) {
      span.setStatus(StatusCode.OK);
    } else {
      span.setStatus(StatusCode.ERROR, message);
    }

    requestCounter.add(1, Objects.requireNonNull(Attributes.builder()
        .put(Objects.requireNonNull(METHOD_KEY), Objects.requireNonNull(method))
        .put(Objects.requireNonNull(STATUS_KEY), Objects.requireNonNull(status))
        .build()));

    requestDurationHistogram.record(duration, Objects.requireNonNull(Attributes.builder()
        .put(Objects.requireNonNull(METHOD_KEY), Objects.requireNonNull(method))
        .put(Objects.requireNonNull(STATUS_KEY), Objects.requireNonNull(status))
        .build()));

    span.end();
  }

  public static class TracingContext {
    private final Context context;
    private final Instant startTime;

    public TracingContext(Context context, Instant startTime) {
      this.context = context;
      this.startTime = startTime;
    }

    public Context getContext() {
      return context;
    }

    public Instant getStartTime() {
      return startTime;
    }
  }
}
