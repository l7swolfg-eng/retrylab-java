package dev.retrylab;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RetryComparisonTest {

    @Test
    void comparesStrategiesUnderSameFailurePattern() throws Exception {

        RetryExecutor.Result none = RetryComparison.runScenario(
                new FixedDelayStrategy(0),
                0,
                2
        );

        RetryExecutor.Result fixed = RetryComparison.runScenario(
                new FixedDelayStrategy(1),
                2,
                2
        );

        RetryExecutor.Result exponential = RetryComparison.runScenario(
                new ExponentialBackoffStrategy(1),
                2,
                2
        );

        // Without retries, the first failure is returned.
        assertEquals(503, none.statusCode());
        assertEquals(1, none.attempts());

        // Both retry strategies recover after two failures.
        assertEquals(200, fixed.statusCode());
        assertEquals(3, fixed.attempts());

        assertEquals(200, exponential.statusCode());
        assertEquals(3, exponential.attempts());

        // Both strategies encounter the same status sequence.
        assertEquals(
                List.of(
                        new RetryExecutor.Attempt(1, 503, 1),
                        new RetryExecutor.Attempt(2, 503, 1),
                        new RetryExecutor.Attempt(3, 200, 0)
                ),
                fixed.history()
        );

        assertEquals(
                List.of(
                        new RetryExecutor.Attempt(1, 503, 1),
                        new RetryExecutor.Attempt(2, 503, 2),
                        new RetryExecutor.Attempt(3, 200, 0)
                ),
                exponential.history()
        );
    }

    @Test
    void eachScenarioStartsWithFreshFailureCounter() throws Exception {

        RetryExecutor.Result first = RetryComparison.runScenario(
                new FixedDelayStrategy(0),
                1,
                1
        );

        RetryExecutor.Result second = RetryComparison.runScenario(
                new FixedDelayStrategy(0),
                1,
                1
        );

        // Both runs should fail once, then succeed.
        assertEquals(200, first.statusCode());
        assertEquals(2, first.attempts());

        assertEquals(200, second.statusCode());
        assertEquals(2, second.attempts());

        assertEquals(first.history(), second.history());
    }

    @Test
    void succeedsImmediatelyWhenNoFailuresAreConfigured() throws Exception {

        RetryExecutor.Result result = RetryComparison.runScenario(
                new ExponentialBackoffStrategy(1),
                2,
                0
        );

        assertEquals(200, result.statusCode());
        assertEquals(1, result.attempts());

        assertEquals(
                List.of(new RetryExecutor.Attempt(1, 200, 0)),
                result.history()
        );
    }
}
