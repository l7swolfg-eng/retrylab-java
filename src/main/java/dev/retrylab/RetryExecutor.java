package dev.retrylab;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RetryExecutor {

    // Stores information about one HTTP attempt.
    public record Attempt(
            int number,
            int statusCode,
            long retryDelayMillis
    ) {
    }

    // Stores the overall result and the history of attempts.
    public record Result(
            int statusCode,
            String body,
            int attempts,
            long elapsedMillis,
            List<Attempt> history
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

        List<Attempt> history = new ArrayList<>();

        while (true) {
            attempts++;

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            int status = response.statusCode();

            System.out.println("Attempt " + attempts + ": HTTP " + status);

            // Stop if the response is not retryable or retries are exhausted.
            if (status != 503 || attempts > maxRetries) {
                history.add(new Attempt(attempts, status, 0));

                long elapsedNanos = System.nanoTime() - startTime;
                long elapsedMillis =
                        TimeUnit.NANOSECONDS.toMillis(elapsedNanos);

                return new Result(
                        status,
                        response.body(),
                        attempts,
                        elapsedMillis,
                        List.copyOf(history)
                );
            }

            long delay = strategy.delayMillis(attempts);

            // Record the delay scheduled after this failed attempt.
            history.add(new Attempt(attempts, status, delay));

            System.out.println("Waiting " + delay + " ms...");
            Thread.sleep(delay);
        }
    }
}
