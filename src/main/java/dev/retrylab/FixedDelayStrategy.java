package dev.retrylab;

public class FixedDelayStrategy implements RetryStrategy {

    private final long delayMillis;

    public FixedDelayStrategy(long delayMillis) {
        if (delayMillis < 0) {
            throw new IllegalArgumentException("Delay cannot be negative");
        }

        this.delayMillis = delayMillis;
    }

    @Override
    public long delayMillis(int retryNumber) {
        if (retryNumber < 1) {
            throw new IllegalArgumentException("Invalid retry number");
        }

        return delayMillis;
    }
}
