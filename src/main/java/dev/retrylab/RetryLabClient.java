package dev.retrylab;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class RetryLabClient {

    public static void main(String[] args)
            throws IOException, InterruptedException {

        int maxRetries = 2;
        long delayMillis = 1000;

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
                System.out.println("Waiting " + delayMillis + " ms...");
                Thread.sleep(delayMillis);
            }
        }

        System.out.println("Failed after " + (maxRetries + 1) + " attempts.");
    }
}
