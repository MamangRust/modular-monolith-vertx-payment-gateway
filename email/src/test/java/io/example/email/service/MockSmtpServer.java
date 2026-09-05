package io.example.email.service;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Minimal SMTP server used by integration tests. Speaks just enough of the
 * dialog (220/EHLO/MAIL FROM/RCPT TO/DATA/QUIT) for vertx-mail-client, and
 * counts delivered messages. {@code failNextConnections(n)} closes the next
 * {@code n} incoming connections without greeting — simulating SMTP being down.
 */
class MockSmtpServer {

  private final Vertx vertx;
  private final AtomicInteger delivered = new AtomicInteger();
  private final AtomicReference<String> lastMessage = new AtomicReference<>();
  private final AtomicInteger failNextConnections = new AtomicInteger();
  private final AtomicInteger connections = new AtomicInteger();

  private NetServer server;
  private int port;

  MockSmtpServer(Vertx vertx) {
    this.vertx = vertx;
  }

  void start() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    server = vertx.createNetServer();
    server.connectHandler(this::handle);
    server.listen(0, "localhost", ar -> {
      if (ar.succeeded()) {
        port = ar.result().actualPort();
      }
      latch.countDown();
    });
    latch.await(5, TimeUnit.SECONDS);
  }

  void stop() {
    if (server != null) {
      server.close();
    }
  }

  int port() {
    return port;
  }

  int delivered() {
    return delivered.get();
  }

  String lastMessage() {
    return lastMessage.get();
  }

  void reset() {
    delivered.set(0);
    lastMessage.set(null);
    failNextConnections.set(0);
    connections.set(0);
  }

  int connections() {
    return connections.get();
  }

  void failNextConnections(int n) {
    failNextConnections.set(n);
  }

  private void handle(NetSocket socket) {
    connections.incrementAndGet();
    if (failNextConnections.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
      // Simulate SMTP being down: refuse the service at the greeting.
      socket.write("554 service unavailable\r\n");
      socket.close();
      return;
    }

    socket.write("220 mock ESMTP ready\r\n");
    StringBuilder[] buffer = {new StringBuilder()};
    StringBuilder[] message = {new StringBuilder()};
    boolean[] inData = {false};

    socket.handler(chunk -> {
      buffer[0].append(chunk.toString());
      int idx;
      while ((idx = buffer[0].indexOf("\n")) >= 0) {
        String line = buffer[0].substring(0, idx).replace("\r", "").trim();
        buffer[0].delete(0, idx + 1);

        if (inData[0]) {
          if (".".equals(line)) {
            inData[0] = false;
            delivered.incrementAndGet();
            lastMessage.set(message[0].toString());
            message[0] = new StringBuilder();
            socket.write("250 OK\r\n");
          } else {
            message[0].append(line).append("\n");
          }
          continue;
        }

        if (line.isEmpty()) {
          continue;
        }
        String upper = line.toUpperCase();
        if (upper.startsWith("EHLO")) {
          socket.write("250-mock\r\n250 OK\r\n");
        } else if (upper.startsWith("MAIL FROM")) {
          socket.write("250 OK\r\n");
        } else if (upper.startsWith("RCPT TO")) {
          socket.write("250 OK\r\n");
        } else if (upper.startsWith("DATA")) {
          socket.write("354 go ahead\r\n");
          inData[0] = true;
        } else if (upper.startsWith("QUIT")) {
          socket.write("250 bye\r\n");
          socket.close();
        } else {
          socket.write("250 OK\r\n");
        }
      }
    });
  }
}
