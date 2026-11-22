package dev.retrylab;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RetryStrategyTest {

    @Test
    void fixedDelayStaysConstant() {
        RetryStrategy strategy = new FixedDelayStrategy(1000);

        assertEquals(1000, strategy.delayMillis(1));
        assertEquals(1000, strategy.delayMillis(2));
        assertEquals(1000, strategy.delayMillis(5));
    }

    @Test
    void exponentialBackoffDoublesDelay() {
        RetryStrategy strategy = new ExponentialBackoffStrategy(1000);

        assertEquals(1000, strategy.delayMillis(1));
        assertEquals(2000, strategy.delayMillis(2));
        assertEquals(4000, strategy.delayMillis(3));
        assertEquals(8000, strategy.delayMillis(4));
    }

    @Test
    void exponentialBackoffCapsAtThirtySeconds() {
        RetryStrategy strategy = new ExponentialBackoffStrategy(1000);

        assertEquals(16000, strategy.delayMillis(5));
        assertEquals(30000, strategy.delayMillis(6));
        assertEquals(30000, strategy.delayMillis(10));
    }

    @Test
    void rejectsNegativeDelays() {
        assertThrows(IllegalArgumentException.class,
                () -> new FixedDelayStrategy(-100));

        assertThrows(IllegalArgumentException.class,
                () -> new ExponentialBackoffStrategy(-100));
    }

    @Test
    void rejectsInvalidRetryNumbers() {
        RetryStrategy fixed = new FixedDelayStrategy(1000);
        RetryStrategy exponential = new ExponentialBackoffStrategy(1000);

        assertThrows(IllegalArgumentException.class,
                () -> fixed.delayMillis(0));

        assertThrows(IllegalArgumentException.class,
                () -> exponential.delayMillis(-1));
    }

    @Test
    void allowsZeroDelay() {
        RetryStrategy fixed = new FixedDelayStrategy(0);
        RetryStrategy exponential = new ExponentialBackoffStrategy(0);

        assertEquals(0, fixed.delayMillis(1));
        assertEquals(0, exponential.delayMillis(5));
    }
}
