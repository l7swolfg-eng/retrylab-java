package dev.retrylab;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RetryExecutorTest {

    @Test
    void retriesTwiceBeforeSuccess() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();

        // Use an automatically assigned port to avoid conflicts.
        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0
        );

        server.createContext("/simulate", exchange -> {
            int attempt = requestCount.incrementAndGet();
            int status = attempt <= 2 ? 503 : 200;

            byte[] body = "Test response".getBytes(StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(status, body.length);

            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });

        server.start();

        try {
            int port = server.getAddress().getPort();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "http://127.0.0.1:" + port + "/simulate"
                    ))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            RetryExecutor.Result result = RetryExecutor.execute(
                    HttpClient.newHttpClient(),
                    request,
                    new FixedDelayStrategy(0),
                    2
            );

            assertEquals(200, result.statusCode());
            assertEquals(3, result.attempts());
            assertEquals(3, requestCount.get());

        } finally {
            server.stop(0);
        }
    }
}
