package dev.retrylab;

import java.util.Locale;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class RetryLabClient {

    public static void main(String[] args)
            throws IOException, InterruptedException {
        if (args.length != 1) {
            System.err.println("Usage: RetryLabClient <none|fixed|exponential>");
            return;
        }

        int maxRetries = 2;
        RetryStrategy strategy;

        String mode = args[0].toLowerCase(Locale.ROOT);

        switch (mode) {
            case "none" -> {
                maxRetries = 0;
                strategy = new FixedDelayStrategy(0);
            }

            case "fixed" -> {
                strategy = new FixedDelayStrategy(1000);
            }

            case "exponential" -> {
                strategy = new ExponentialBackoffStrategy(1000);
            }

            default -> {
                System.err.println("Unknown strategy: " + args[0]);
                System.err.println("Choose none, fixed, or exponential.");
                return;
            }
        }

        System.out.println("Retry strategy: " + mode);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/simulate"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            System.out.println(
                    "Attempt " + attempt + ": HTTP " + response.statusCode()
            );

            if (response.statusCode() == 200) {
                System.out.println("Success: " + response.body());
                return;
            }

            // Only retry temporary 503 errors for now.
            if (response.statusCode() != 503) {
                System.out.println("Non-retryable error. Stopping.");
                return;
            }

            if (attempt <= maxRetries) {
                long delayMillis = strategy.delayMillis(attempt);

                System.out.println("Waiting " + delayMillis + " ms...");
                Thread.sleep(delayMillis);
            }
        }

        System.out.println("Failed after " + (maxRetries + 1) + " attempts.");
    }
}
