package dev.retrylab;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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

        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0
        );

        server.createContext("/simulate", exchange -> {
            int attempt = requestCount.incrementAndGet();
            int status = attempt <= 2 ? 503 : 200;

            byte[] body = "Test response"
                    .getBytes(StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(status, body.length);

            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });

        server.start();

        try {
            RetryExecutor.Result result = RetryExecutor.execute(
                    HttpClient.newHttpClient(),
                    createRequest(server),
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

    @Test
    void noRetrySendsOnlyOneRequest() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();

        HttpServer server = createTestServer(requestCount, 503);
        server.start();

        try {
            RetryExecutor.Result result = RetryExecutor.execute(
                    HttpClient.newHttpClient(),
                    createRequest(server),
                    new FixedDelayStrategy(0),
                    0
            );

            assertEquals(503, result.statusCode());
            assertEquals(1, result.attempts());
            assertEquals(1, requestCount.get());

        } finally {
            server.stop(0);
        }
    }

    @Test
    void nonRetryableErrorStopsImmediately() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();

        HttpServer server = createTestServer(requestCount, 404);
        server.start();

        try {
            RetryExecutor.Result result = RetryExecutor.execute(
                    HttpClient.newHttpClient(),
                    createRequest(server),
                    new FixedDelayStrategy(0),
                    3
            );

            assertEquals(404, result.statusCode());
            assertEquals(1, result.attempts());
            assertEquals(1, requestCount.get());

        } finally {
            server.stop(0);
        }
    }

    @Test
    void stopsAfterMaximumRetries() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();

        HttpServer server = createTestServer(requestCount, 503);
        server.start();

        try {
            RetryExecutor.Result result = RetryExecutor.execute(
                    HttpClient.newHttpClient(),
                    createRequest(server),
                    new FixedDelayStrategy(0),
                    2
            );

            assertEquals(503, result.statusCode());
            assertEquals(3, result.attempts());
            assertEquals(3, requestCount.get());

        } finally {
            server.stop(0);
        }
    }

    private HttpServer createTestServer(
            AtomicInteger requestCount,
            int status
    ) throws IOException {

        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0
        );

        server.createContext("/simulate", exchange -> {
            requestCount.incrementAndGet();

            byte[] body = "Test response"
                    .getBytes(StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(status, body.length);

            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });

        return server;
    }

    private HttpRequest createRequest(HttpServer server) {
        int port = server.getAddress().getPort();

        return HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://127.0.0.1:" + port + "/simulate"
                ))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
    }
}