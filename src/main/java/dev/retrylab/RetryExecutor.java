package dev.retrylab;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RetryExecutor {

    public record Result(int statusCode, String body, int attempts) {
    }

    public static Result execute(
            HttpClient client,
            HttpRequest request,
            RetryStrategy strategy,
            int maxRetries
    ) throws IOException, InterruptedException {

        if (maxRetries < 0) {
            throw new IllegalArgumentException("Retries cannot be negative");
        }

        int attempts = 0;

        while (true) {
            attempts++;

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            int status = response.statusCode();

            System.out.println("Attempt " + attempts + ": HTTP " + status);

            // Stop on success, non-retryable errors, or exhausted retries.
            if (status != 503 || attempts > maxRetries) {
                return new Result(status, response.body(), attempts);
            }

            long delay = strategy.delayMillis(attempts);

            System.out.println("Waiting " + delay + " ms...");
            Thread.sleep(delay);
        }
    }
}
