package dev.retrylab;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

public class RetryExecutor {

    public record Result(
            int statusCode,
            String body,
            int attempts,
            long elapsedMillis
    ) {
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
        long startTime = System.nanoTime();

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
                long elapsedNanos = System.nanoTime() - startTime;
                long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);

                return new Result(
                        status,
                        response.body(),
                        attempts,
                        elapsedMillis
                );
            }

            long delay = strategy.delayMillis(attempts);

            System.out.println("Waiting " + delay + " ms...");
            Thread.sleep(delay);
        }
    }
}
