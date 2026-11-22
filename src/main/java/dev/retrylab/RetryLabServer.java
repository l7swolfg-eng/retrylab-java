package dev.retrylab;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

public class RetryLabServer {

    public static void main(String[] args) throws IOException {

        // Number of requests that should fail before succeeding.
        int failuresBeforeSuccess = 2;

        if (args.length > 1) {
            System.err.println("Usage: RetryLabServer [failures]");
            return;
        }

        if (args.length == 1) {
            try {
                failuresBeforeSuccess = Integer.parseInt(args[0]);

                if (failuresBeforeSuccess < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                System.err.println("Failures must be a non-negative integer.");
                return;
            }
        }

        AtomicInteger requestCount = new AtomicInteger();

        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 8080), 0
        );

        // Health endpoint.
        server.createContext("/health", exchange -> {
            sendResponse(exchange, 200, "RetryLab server is running");
        });

        // Endpoint that intentionally fails before succeeding.
        final int failureLimit = failuresBeforeSuccess;

        server.createContext("/simulate", exchange -> {
            if (!exchange.getRequestMethod().equals("GET")) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            int attempt = requestCount.incrementAndGet();

            if (attempt <= failureLimit) {
                sendResponse(exchange, 503,
                        "Attempt " + attempt + ": Simulated failure");
            } else {
                sendResponse(exchange, 200,
                        "Attempt " + attempt + ": Success");
            }
        });

        server.start();

        System.out.println("RetryLab server started on port 8080");
        System.out.println("Failures before success: " + failureLimit);
    }

    private static void sendResponse(
            HttpExchange exchange,
            int status,
            String message
    ) throws IOException {

        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "text/plain; charset=UTF-8"
        );

        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
