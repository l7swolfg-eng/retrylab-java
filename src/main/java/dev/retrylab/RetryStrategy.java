package dev.retrylab;

public interface RetryStrategy {

    // Returns how long to wait before the given retry.
    long delayMillis(int retryNumber);
}
