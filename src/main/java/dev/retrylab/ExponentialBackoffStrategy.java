package dev.retrylab;

public class ExponentialBackoffStrategy implements RetryStrategy {

    private static final long MAX_DELAY_MILLIS = 30_000;

    private final long initialDelayMillis;

    public ExponentialBackoffStrategy(long initialDelayMillis) {
        if (initialDelayMillis < 0) {
            throw new IllegalArgumentException("Delay cannot be negative");
        }

        this.initialDelayMillis = initialDelayMillis;
    }

    @Override
    public long delayMillis(int retryNumber) {
        if (retryNumber < 1) {
            throw new IllegalArgumentException("Invalid retry number");
        }

        long delay = Math.min(initialDelayMillis, MAX_DELAY_MILLIS);

        for (int i = 1; i < retryNumber; i++) {
            delay = Math.min(delay * 2, MAX_DELAY_MILLIS);
        }

        return delay;
    }
}
