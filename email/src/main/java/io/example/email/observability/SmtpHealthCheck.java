package io.example.email.observability;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SMTP connectivity health check (Phase 5 baseline). Opens a raw TCP connection
 * to the SMTP server and completes a greeting ({@code 220}) + {@code EHLO}
 * ({@code 250}) round-trip within a configurable timeout — the same SMTP server
 * the {@code MailClient} talks to, without sending any mail.
 *
 * <p>Enabled via {@code smtp.health.enabled} (default {@code true}); the timeout
 * is {@code smtp.health.timeout-ms} (default 3000).
 */
public class SmtpHealthCheck {

  private static final Logger log = LoggerFactory.getLogger(SmtpHealthCheck.class);

  private enum State { GREETING, EHLO }

  private final Vertx vertx;
  private final String host;
  private final int port;
  private final long timeoutMs;
  private final String heloName;

  public SmtpHealthCheck(Vertx vertx, String host, int port, long timeoutMs) {
    this.vertx = vertx;
    this.host = host;
    this.port = port;
    this.timeoutMs = timeoutMs;
    this.heloName = "email-service";
  }

  /** {@code true} when the SMTP server completes a greeting + EHLO round-trip. */
  public Future<Boolean> check() {
    Promise<Boolean> promise = Promise.promise();
    NetClient client = vertx.createNetClient(new NetClientOptions().setConnectTimeout((int) timeoutMs));

    client.connect(port, host, ar -> {
      if (ar.failed()) {
        client.close();
        promise.complete(false);
        return;
      }
      NetSocket socket = ar.result();
      State[] state = {State.GREETING};
      StringBuilder buffer = new StringBuilder();
      long timeoutId = vertx.setTimer(timeoutMs, id -> fail(promise, socket, client, "timeout"));

      socket.exceptionHandler(err -> fail(promise, socket, client, err.getMessage()));
      socket.closeHandler(v -> {
        client.close();
        if (!promise.future().isComplete()) {
          promise.complete(false);
        }
      });
      socket.handler(chunk -> {
        buffer.append(chunk.toString());
        int idx;
        while ((idx = buffer.indexOf("\n")) >= 0) {
          String line = buffer.substring(0, idx).replace("\r", "").trim();
          buffer.delete(0, idx + 1);
          if (line.isEmpty()) {
            continue;
          }
          if (state[0] == State.GREETING) {
            if (line.startsWith("220")) {
              socket.write("EHLO " + heloName + "\r\n");
              state[0] = State.EHLO;
            } else {
              fail(promise, socket, client, "unexpected greeting: " + line);
              return;
            }
          } else {
            if (line.startsWith("250 ")) {
              vertx.cancelTimer(timeoutId);
              socket.write("QUIT\r\n");
              socket.close();
              client.close();
              promise.complete(true);
              return;
            } else if (!line.startsWith("250-")) {
              fail(promise, socket, client, "unexpected EHLO response: " + line);
              return;
            }
          }
        }
      });
    });
    return promise.future();
  }

  private static void fail(Promise<Boolean> promise, NetSocket socket, NetClient client, String reason) {
    if (promise.future().isComplete()) {
      return;
    }
    log.debug("SMTP health check failed: {}", reason);
    socket.close();
    client.close();
    promise.complete(false);
  }
}
