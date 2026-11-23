package dev.retrylab;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExponentialJitterStrategyTest {

    @Test
    void delayStaysWithinExponentialBounds() {
        ExponentialJitterStrategy strategy =
                new ExponentialJitterStrategy(100);

        for (int retryNumber = 1; retryNumber <= 6; retryNumber++) {
            long maxDelay = 100L << (retryNumber - 1);

            // Check multiple random samples for each retry.
            for (int i = 0; i < 100; i++) {
                long delay = strategy.delayMillis(retryNumber);

                assertTrue(delay >= 0);
                assertTrue(delay <= maxDelay);
            }
        }
    }

    @Test
    void delayNeverExceedsThirtySecondCap() {
        ExponentialJitterStrategy strategy =
                new ExponentialJitterStrategy(10_000);

        int[] retryNumbers = {1, 2, 3, 10, 100};
        long[] expectedMaximums = {
                10_000, 20_000, 30_000, 30_000, 30_000
        };

        for (int i = 0; i < retryNumbers.length; i++) {
            for (int sample = 0; sample < 100; sample++) {
                long delay = strategy.delayMillis(retryNumbers[i]);

                assertTrue(delay >= 0);
                assertTrue(delay <= expectedMaximums[i]);
            }
        }
    }

    @Test
    void zeroInitialDelayAlwaysReturnsZero() {
        ExponentialJitterStrategy strategy =
                new ExponentialJitterStrategy(0);

        assertEquals(0, strategy.delayMillis(1));
        assertEquals(0, strategy.delayMillis(2));
        assertEquals(0, strategy.delayMillis(10));
        assertEquals(0, strategy.delayMillis(100));
    }

    @Test
    void rejectsNegativeInitialDelay() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ExponentialJitterStrategy(-1)
        );
    }

    @Test
    void rejectsInvalidRetryNumbers() {
        ExponentialJitterStrategy strategy =
                new ExponentialJitterStrategy(100);

        assertThrows(
                IllegalArgumentException.class,
                () -> strategy.delayMillis(0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> strategy.delayMillis(-1)
        );
    }

    @Test
    void veryLargeInitialDelayIsCapped() {
        ExponentialJitterStrategy strategy =
                new ExponentialJitterStrategy(Long.MAX_VALUE);

        for (int i = 0; i < 100; i++) {
            long delay = strategy.delayMillis(5);

            assertTrue(delay >= 0);
            assertTrue(delay <= 30_000);
        }
    }

    @Test
    void jitterWorksWithRetryComparison() throws Exception {
        RetryExecutor.Result result = RetryComparison.runScenario(
                new ExponentialJitterStrategy(10),
                2,
                2
        );

        assertEquals(200, result.statusCode());
        assertEquals(3, result.attempts());

        List<RetryExecutor.Attempt> history = result.history();

        assertEquals(3, history.size());

        assertEquals(503, history.get(0).statusCode());
        assertEquals(503, history.get(1).statusCode());
        assertEquals(200, history.get(2).statusCode());

        // First retry can wait between 0 and 10 ms.
        assertTrue(history.get(0).retryDelayMillis() >= 0);
        assertTrue(history.get(0).retryDelayMillis() <= 10);

        // Second retry can wait between 0 and 20 ms.
        assertTrue(history.get(1).retryDelayMillis() >= 0);
        assertTrue(history.get(1).retryDelayMillis() <= 20);

        // No retry follows the successful request.
        assertEquals(0, history.get(2).retryDelayMillis());
    }
}
