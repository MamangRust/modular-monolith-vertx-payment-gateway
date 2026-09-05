package io.example.email.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

class SmtpHealthCheckTest {

  private Vertx vertx;

  @BeforeEach
  void setUp() {
    vertx = Vertx.vertx();
  }

  @AfterEach
  void tearDown() {
    vertx.close();
  }

  private int startServer(Handler<NetSocket> connectHandler) throws Exception {
    CompletableFuture<Integer> port = new CompletableFuture<>();
    NetServer server = vertx.createNetServer(new NetServerOptions());
    server.connectHandler(connectHandler);
    server.listen(0, "localhost", ar -> {
      if (ar.succeeded()) {
        port.complete(ar.result().actualPort());
      } else {
        port.completeExceptionally(ar.cause());
      }
    });
    return port.get(5, TimeUnit.SECONDS);
  }

  private boolean runCheck(SmtpHealthCheck check) throws Exception {
    return check.check().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  @Test
  void check_shouldPassWithGreetingAndEhlo() throws Exception {
    int port = startServer(socket -> {
      socket.write("220 localhost ESMTP\r\n");
      socket.handler(buffer -> {
        String data = buffer.toString();
        if (data.startsWith("EHLO")) {
          socket.write("250-email-service\r\n250 OK\r\n");
        } else if (data.startsWith("QUIT")) {
          socket.close();
        }
      });
    });

    boolean ok = runCheck(new SmtpHealthCheck(vertx, "localhost", port, 2_000));
    assertThat(ok).isTrue();
  }

  @Test
  void check_shouldFailWhenServerClosesImmediately() throws Exception {
    int port = startServer(NetSocket::close);

    boolean ok = runCheck(new SmtpHealthCheck(vertx, "localhost", port, 2_000));
    assertThat(ok).isFalse();
  }

  @Test
  void check_shouldFailWhenPortUnreachable() throws Exception {
    boolean ok = runCheck(new SmtpHealthCheck(vertx, "localhost", 1, 2_000));
    assertThat(ok).isFalse();
  }

  @Test
  void check_shouldFailOnUnexpectedGreeting() throws Exception {
    int port = startServer(socket -> socket.write("554 no service\r\n"));

    boolean ok = runCheck(new SmtpHealthCheck(vertx, "localhost", port, 2_000));
    assertThat(ok).isFalse();
  }
}
