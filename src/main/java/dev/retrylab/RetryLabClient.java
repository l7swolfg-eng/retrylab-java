package dev.retrylab;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Locale;

public class RetryLabClient {

    public static void main(String[] args)
            throws IOException, InterruptedException {

        if (args.length != 1) {
            System.err.println(
                    "Usage: RetryLabClient <none|fixed|exponential>"
            );
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
                System.err.println(
                        "Choose none, fixed, or exponential."
                );
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

        RetryExecutor.Result result = RetryExecutor.execute(
                client,
                request,
                strategy,
                maxRetries
        );

        if (result.statusCode() == 200) {
            System.out.println("Success: " + result.body());

        } else if (result.statusCode() == 503) {
            System.out.println(
                    "Failed after " + result.attempts() + " attempt(s)."
            );

        } else {
            System.out.println(
                    "Non-retryable error: HTTP " + result.statusCode()
            );
        }

        System.out.println();
        System.out.println("Total attempts: " + result.attempts());
        System.out.println(
                "Total elapsed time: " + result.elapsedMillis() + " ms"
        );
    }
}
