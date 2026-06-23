package io.example.common.chaos;

import io.grpc.MethodDescriptor;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientRequest;
import io.vertx.grpc.client.GrpcClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * JDK dynamic proxy that wraps a {@link GrpcClient} and intercepts <b>outbound</b>
 * gRPC calls to inject latency or failure according to chaos policies of type
 * {@code "grpc"}.
 *
 * <p>The interceptor works at two levels:
 * <ol>
 *   <li><b>Address-level</b> — matches the backend address ({@code host:port})
 *       against the policy target.</li>
 *   <li><b>Method-level</b> — once the method descriptor is set on the request
 *       (via {@link GrpcClientRequest#method(MethodDescriptor)}), it extracts
 *       the full gRPC method path ({@code package.Service/Method}) and re-evaluates
 *       for a more specific match.</li>
 * </ol>
 *
 * <p>When a policy matches:
 * <ul>
 *   <li>{@code latencyMs} — the outbound call is delayed before being forwarded</li>
 *   <li>{@code grpcStatus} / {@code errorMessage} — the call is failed immediately
 *       with the corresponding gRPC error, without reaching the backend</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * GrpcClient raw = GrpcClient.client(vertx);
 * GrpcClient chaos = ChaosGrpcClientInterceptor.wrap(raw, chaosManager, vertx);
 * var stub = new pb.user.VertxUserQueryServiceGrpcClient(chaos, addr);
 * }</pre>
 */
public class ChaosGrpcClientInterceptor implements InvocationHandler {
  private static final Logger log = LoggerFactory.getLogger(ChaosGrpcClientInterceptor.class);

  private final GrpcClient delegate;
  private final ChaosManager manager;
  private final Vertx vertx;

  public ChaosGrpcClientInterceptor(GrpcClient delegate, ChaosManager manager, Vertx vertx) {
    this.delegate = delegate;
    this.manager = manager;
    this.vertx = vertx;
  }

  /**
   * Wrap a {@link GrpcClient} with chaos interception for outbound calls.
   */
  public static GrpcClient wrap(GrpcClient client, ChaosManager manager, Vertx vertx) {
    return (GrpcClient) Proxy.newProxyInstance(
        GrpcClient.class.getClassLoader(),
        new Class<?>[]{GrpcClient.class},
        new ChaosGrpcClientInterceptor(client, manager, vertx));
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String methodName = method.getName();

    if ("request".equals(methodName) && args != null && args.length == 1 && args[0] instanceof SocketAddress) {
      SocketAddress addr = (SocketAddress) args[0];
      String addressTarget = addr.host() + ":" + addr.port();

      // Try address-based match for address-level chaos
      ChaosPolicy addrPolicy = manager.evaluate("grpc", addressTarget);

      if (addrPolicy != null && addrPolicy.isEnabled() && Math.random() < addrPolicy.getErrorChance()) {
        if (addrPolicy.getGrpcStatus() != null || addrPolicy.getErrorMessage() != null) {
          log.info("🔥 Injecting gRPC client chaos [Policy: {}] for backend: {}",
              addrPolicy.getName(), addressTarget);
          throw buildGrpcError(addrPolicy);
        }
        // Pure latency — will be handled by the method interceptor below
      }

      // Create the real request, then wrap it with method-level interception
      GrpcClientRequest<Object, Object> realRequest =
          (GrpcClientRequest<Object, Object>) method.invoke(delegate, args);
      return wrapClientRequest(realRequest, addressTarget, addrPolicy);
    }

    try {
      return method.invoke(delegate, args);
    } catch (Exception e) {
      throw e.getCause() != null ? e.getCause() : e;
    }
  }

  @SuppressWarnings("unchecked")
  private GrpcClientRequest<Object, Object> wrapClientRequest(
      GrpcClientRequest<Object, Object> realRequest, String addressTarget, ChaosPolicy addrPolicy) {

    return (GrpcClientRequest<Object, Object>) Proxy.newProxyInstance(
        GrpcClientRequest.class.getClassLoader(),
        new Class<?>[]{GrpcClientRequest.class},
        new RequestInvocationHandler(realRequest, addressTarget, addrPolicy, manager, vertx));
  }

  /**
   * Invocation handler for the wrapped {@link GrpcClientRequest}.
   * Intercepts {@code method(MethodDescriptor)} to capture the gRPC method name,
   * then intercepts {@code response()} and the terminal write operations to
   * inject latency or faults.
   */
  private static class RequestInvocationHandler implements InvocationHandler {
    private final GrpcClientRequest<Object, Object> realRequest;
    private final String addressTarget;
    private final ChaosPolicy addrPolicy;
    private final ChaosManager manager;
    private final Vertx vertx;
    private volatile String grpcMethod;

    RequestInvocationHandler(GrpcClientRequest<Object, Object> realRequest,
                             String addressTarget,
                             ChaosPolicy addrPolicy,
                             ChaosManager manager,
                             Vertx vertx) {
      this.realRequest = realRequest;
      this.addressTarget = addressTarget;
      this.addrPolicy = addrPolicy;
      this.manager = manager;
      this.vertx = vertx;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      String name = method.getName();

      // Capture the gRPC method name when it is set on the request
      if ("method".equals(name) && args != null && args.length == 1 && args[0] instanceof MethodDescriptor) {
        MethodDescriptor<?, ?> desc = (MethodDescriptor<?, ?>) args[0];
        // Full gRPC method path, e.g. "pb.AuthService/LoginUser"
        this.grpcMethod = desc.getFullMethodName();
        return safeInvoke(realRequest, method, args);
      }

      // Intercept the response future to inject chaos
      if ("response".equals(name)) {
        return wrapResponse();
      }

      return safeInvoke(realRequest, method, args);
    }

    private Object wrapResponse() {
      // Re-evaluate with full method path for better matching
      final ChaosPolicy resolvedPolicy;
      if (grpcMethod != null) {
        ChaosPolicy matched = manager.evaluate("grpc", grpcMethod);
        if (matched != null) {
          resolvedPolicy = matched;
        } else {
          resolvedPolicy = addrPolicy;
        }
      } else {
        resolvedPolicy = addrPolicy;
      }

      ChaosPolicy policy = resolvedPolicy;
      if (policy != null && policy.isEnabled() && policy.getErrorChance() > 0
          && Math.random() < policy.getErrorChance()) {
        log.info("🔥 Injecting gRPC client chaos [Policy: {}] to method: {}",
            policy.getName(), grpcMethod != null ? grpcMethod : addressTarget);

        long latency = policy.getLatencyMs();
        if (policy.getGrpcStatus() != null || policy.getErrorMessage() != null) {
          // Return a failed future immediately
          Promise<GrpcClientResponse<Object, Object>> promise = Promise.promise();
          if (latency > 0) {
            vertx.setTimer(latency, id ->
                promise.fail(buildGrpcError(policy)));
          } else {
            promise.fail(buildGrpcError(policy));
          }
          return promise.future();
        } else if (latency > 0) {
          // Return a delayed future
          Promise<GrpcClientResponse<Object, Object>> promise = Promise.promise();
          vertx.setTimer(latency, id -> {
            try {
              Future<GrpcClientResponse<Object, Object>> real = (Future<GrpcClientResponse<Object, Object>>)
                  realRequest.getClass().getMethod("response").invoke(realRequest);
              real.onComplete(ar -> {
                if (ar.succeeded()) promise.complete(ar.result());
                else promise.fail(ar.cause());
              });
            } catch (Exception e) {
              promise.fail(e.getCause() != null ? e.getCause() : e);
            }
          });
          return promise.future();
        }
      }

      // No chaos — delegate
      try {
        return safeInvoke(realRequest, realRequest.getClass().getMethod("response"));
      } catch (Exception e) {
        return Future.failedFuture(e.getCause() != null ? e.getCause() : e);
      }
    }

    private Object safeInvoke(Object target, Method m, Object... args) throws Exception {
      try {
        return m.invoke(target, args);
      } catch (Exception e) {
        throw e.getCause() != null ? (Exception) e.getCause() : e;
      }
    }
  }

  /**
   * Build a {@link RuntimeException} carrying gRPC status info,
   * matching the format used by the server-side interceptor.
   */
  private static RuntimeException buildGrpcError(ChaosPolicy policy) {
    String msg = policy.getErrorMessage() != null
        ? policy.getErrorMessage()
        : "Simulated gRPC client chaos fault";
    int grpcCode = grpcStatusCode(policy.getGrpcStatus());
    return new RuntimeException("grpc-status:" + grpcCode + ", grpc-message:" + msg);
  }

  private static int grpcStatusCode(String statusName) {
    if (statusName == null) return 14; // UNAVAILABLE
    return switch (statusName.toUpperCase()) {
      case "OK" -> 0;
      case "CANCELLED" -> 1;
      case "UNKNOWN" -> 2;
      case "INVALID_ARGUMENT" -> 3;
      case "DEADLINE_EXCEEDED" -> 4;
      case "NOT_FOUND" -> 5;
      case "ALREADY_EXISTS" -> 6;
      case "PERMISSION_DENIED" -> 7;
      case "RESOURCE_EXHAUSTED" -> 8;
      case "FAILED_PRECONDITION" -> 9;
      case "ABORTED" -> 10;
      case "OUT_OF_RANGE" -> 11;
      case "UNIMPLEMENTED" -> 12;
      case "INTERNAL" -> 13;
      case "UNAVAILABLE" -> 14;
      case "DATA_LOSS" -> 15;
      case "UNAUTHENTICATED" -> 16;
      default -> 14;
    };
  }
}
