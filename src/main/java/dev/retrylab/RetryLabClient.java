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
        System.out.println();

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
            System.out.println("\nSuccess: " + result.body());

        } else if (result.statusCode() == 503) {
            System.out.println(
                    "\nFailed after " + result.attempts() + " attempt(s)."
            );

        } else {
            System.out.println(
                    "\nNon-retryable error: HTTP " + result.statusCode()
            );
        }

        printSummary(result);
    }

    private static void printSummary(RetryExecutor.Result result) {

        System.out.println("\n----- Retry Performance Summary -----\n");

        System.out.printf(
                "%-10s %-10s %-15s%n",
                "Attempt", "Status", "Retry Delay"
        );

        for (int i = 0; i < result.history().size(); i++) {
            RetryExecutor.Attempt attempt = result.history().get(i);

            // No delay follows the final HTTP attempt.
            String delay = i == result.history().size() - 1
                    ? "-"
                    : attempt.retryDelayMillis() + " ms";

            System.out.printf(
                    "%-10d %-10d %-15s%n",
                    attempt.number(),
                    attempt.statusCode(),
                    delay
            );
        }

        System.out.println();
        System.out.println("Total attempts: " + result.attempts());
        System.out.println(
                "Total elapsed time: " + result.elapsedMillis() + " ms"
        );
        System.out.println("Final status: " + result.statusCode());
    }
}
