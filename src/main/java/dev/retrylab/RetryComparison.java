package dev.retrylab;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public class RetryComparison {

    public static void main(String[] args)
            throws IOException, InterruptedException {

        // Default simulation settings.
        int failuresBeforeSuccess = 2;
        int maxRetries = 2;
        long initialDelayMillis = 1000;

        // Accept either no arguments or exactly three.
        if (args.length != 0 && args.length != 3) {
            printUsage();
            return;
        }

        if (args.length == 3) {
            try {
                failuresBeforeSuccess = Integer.parseInt(args[0]);
                maxRetries = Integer.parseInt(args[1]);
                initialDelayMillis = Long.parseLong(args[2]);

            } catch (NumberFormatException e) {
                System.err.println("All arguments must be integers.");
                printUsage();
                return;
            }

            if (failuresBeforeSuccess < 0
                    || maxRetries < 0
                    || initialDelayMillis < 0) {

                System.err.println("Arguments cannot be negative.");
                return;
            }
        }

        System.out.println("RetryLab - Strategy Comparison");
        System.out.println("Simulated failures: " + failuresBeforeSuccess);
        System.out.println("Maximum retries: " + maxRetries);
        System.out.println("Initial delay: " + initialDelayMillis + " ms");

        System.out.println("\n--- No Retry ---");
        RetryExecutor.Result none = runScenario(
                new FixedDelayStrategy(0),
                0,
                failuresBeforeSuccess
        );

        System.out.println("\n--- Fixed Delay ---");
        RetryExecutor.Result fixed = runScenario(
                new FixedDelayStrategy(initialDelayMillis),
                maxRetries,
                failuresBeforeSuccess
        );

        System.out.println("\n--- Exponential Backoff ---");
        RetryExecutor.Result exponential = runScenario(
                new ExponentialBackoffStrategy(initialDelayMillis),
                maxRetries,
                failuresBeforeSuccess
        );

        System.out.println("\n----- Comparison Results -----");

        System.out.printf(
                "%-15s %-10s %-10s %-12s%n",
                "Strategy", "Status", "Attempts", "Time (ms)"
        );

        printResult("None", none);
        printResult("Fixed", fixed);
        printResult("Exponential", exponential);
    }

    static RetryExecutor.Result runScenario(
            RetryStrategy strategy,
            int maxRetries,
            int failuresBeforeSuccess
    ) throws IOException, InterruptedException {

        AtomicInteger requestCount = new AtomicInteger();

        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0
        );

        server.createContext("/simulate", exchange -> {
            int attempt = requestCount.incrementAndGet();

            int status = attempt <= failuresBeforeSuccess
                    ? 503
                    : 200;

            String message = status == 503
                    ? "Simulated failure"
                    : "Success";

            byte[] body = message.getBytes(StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(status, body.length);

            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });

        server.start();

        try {
            int port = server.getAddress().getPort();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "http://127.0.0.1:" + port + "/simulate"
                    ))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            return RetryExecutor.execute(
                    HttpClient.newHttpClient(),
                    request,
                    strategy,
                    maxRetries
            );

        } finally {
            server.stop(0);
        }
    }

    private static void printResult(
            String name,
            RetryExecutor.Result result
    ) {
        System.out.printf(
                "%-15s %-10d %-10d %-12d%n",
                name,
                result.statusCode(),
                result.attempts(),
                result.elapsedMillis()
        );
    }

    private static void printUsage() {
        System.err.println(
                "Usage: RetryComparison "
                        + "[failuresBeforeSuccess maxRetries initialDelayMillis]"
        );
    }
}
