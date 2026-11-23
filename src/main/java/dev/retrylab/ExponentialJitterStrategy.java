package dev.retrylab;

import java.util.concurrent.ThreadLocalRandom;

public class ExponentialJitterStrategy implements RetryStrategy {

    private static final long MAX_DELAY_MILLIS = 30_000;

    private final long initialDelayMillis;

    public ExponentialJitterStrategy(long initialDelayMillis) {
        if (initialDelayMillis < 0) {
            throw new IllegalArgumentException(
                    "Initial delay cannot be negative"
            );
        }

        this.initialDelayMillis = initialDelayMillis;
    }

    @Override
    public long delayMillis(int retryNumber) {
        if (retryNumber < 1) {
            throw new IllegalArgumentException(
                    "Retry number must be at least 1"
            );
        }

        long maxDelay = Math.min(
                initialDelayMillis,
                MAX_DELAY_MILLIS
        );

        if (maxDelay == 0) {
            return 0;
        }

        // Double the delay limit for each additional retry.
        for (int i = 1;
             i < retryNumber && maxDelay < MAX_DELAY_MILLIS;
             i++) {

            maxDelay = Math.min(
                    maxDelay * 2,
                    MAX_DELAY_MILLIS
            );
        }

        // Full jitter: choose a random delay from 0 to maxDelay.
        return ThreadLocalRandom.current().nextLong(maxDelay + 1);
    }
}
