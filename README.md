# RetryLab

A Java-based HTTP retry simulator for exploring and comparing retry strategies under controlled failure conditions.

RetryLab runs a local HTTP server that simulates temporary service failures, then measures how different retry strategies respond. It supports fixed delays, exponential backoff, randomized jitter, and no-retry behavior.

## Features

- Local HTTP server that simulates configurable HTTP 503 failures
- Four retry strategies:
    - No retry
    - Fixed delay
    - Exponential backoff
    - Exponential backoff with full jitter
- Configurable retry limits and delay settings
- Automatic strategy comparison using independent failure scenarios
- Per-attempt tracking of HTTP status codes and retry delays
- Total execution time measurement
- JUnit unit and HTTP integration tests

## Requirements

- Java 21 or later
- Maven 3.x

## Getting Started

Clone the repository and navigate to the project directory.

Build the project and run its tests:

```bash
mvn clean test
```

Maven compiles the source files and executes the JUnit tests.

## Usage

### 1. Start the simulated HTTP server

```bash
java -cp target/classes dev.retrylab.RetryLabServer 2
```

The argument `2` configures the server to return HTTP 503 for its first two requests to `/simulate`. Subsequent requests return HTTP 200.

The server listens on `127.0.0.1:8080`.

Available endpoints:

- `GET /health` - Returns HTTP 200 for a health check.
- `GET /simulate` - Simulates failures followed by success.

Stop the server with `Ctrl+C`.

### 2. Run the HTTP client

Open a second terminal and choose a strategy:

No retries:

```bash
java -cp target/classes dev.retrylab.RetryLabClient none
```

Fixed-delay retries:

```bash
java -cp target/classes dev.retrylab.RetryLabClient fixed
```

Exponential backoff:

```bash
java -cp target/classes dev.retrylab.RetryLabClient exponential
```

The client allows up to two retries for the fixed and exponential strategies. Fixed delay waits 1,000 ms between retries, while exponential backoff starts at 1,000 ms and doubles the delay.

Restart the server between manual experiments to reset its failure counter.

### 3. Compare all strategies automatically

RetryLab includes a comparison tool that runs each strategy against a fresh simulated server.

```bash
java -cp target/classes dev.retrylab.RetryComparison
```

Default configuration:

- 2 simulated failures before success
- 2 maximum retries
- 1,000 ms initial delay

To customize these settings:

```bash
java -cp target/classes dev.retrylab.RetryComparison 3 4 500
```

Arguments:

1. `failuresBeforeSuccess` - Number of initial HTTP 503 responses
2. `maxRetries` - Maximum retries after the first request
3. `initialDelayMillis` - Starting retry delay in milliseconds

The comparison includes all four strategies and reports HTTP status, attempt count, and total elapsed time.

Unlike the standalone client, the comparison tool creates and stops its own HTTP servers. No second terminal is required.

## Retry Strategies

### No Retry

Sends a single request and returns the response without retrying.

### Fixed Delay

Waits a constant amount of time between retries.

For a 1,000 ms delay:

```text
Retry 1: 1000 ms
Retry 2: 1000 ms
Retry 3: 1000 ms
```

### Exponential Backoff

Doubles the delay after each unsuccessful retry, up to a maximum of 30,000 ms.

For a 1,000 ms initial delay:

```text
Retry 1: 1000 ms
Retry 2: 2000 ms
Retry 3: 4000 ms
Retry 4: 8000 ms
```

### Exponential Backoff with Full Jitter

Selects a random delay between zero and the current exponential backoff limit.

For a 1,000 ms initial delay:

```text
Retry 1: 0-1000 ms
Retry 2: 0-2000 ms
Retry 3: 0-4000 ms
Retry 4: 0-8000 ms
```

Jitter helps distribute retry attempts over time when multiple clients encounter failures simultaneously.

## Example Comparison

With two simulated failures and two allowed retries:

| Strategy | Final Status | Attempts | Retry Delays |
|----------|--------------|----------|--------------|
| None | 503 | 1 | None |
| Fixed | 200 | 3 | 1000, 1000 ms |
| Exponential | 200 | 3 | 1000, 2000 ms |
| Exponential Jitter | 200 | 3 | Randomized |

Actual elapsed times vary depending on HTTP processing and the selected delays.

The comparison provides an illustrative demonstration of retry tradeoffs, not a production-scale performance benchmark.

## Project Architecture

```text
RetryLabServer
    |
    | Simulates HTTP failures
    v
RetryExecutor
    |
    | Uses a RetryStrategy
    v
RetryStrategy
    |
    +-- FixedDelayStrategy
    |
    +-- ExponentialBackoffStrategy
    |
    +-- ExponentialJitterStrategy
```

### Main Components

- **RetryLabServer:** Provides HTTP endpoints and simulates failures.
- **RetryLabClient:** Runs individual retry strategies through the command line.
- **RetryExecutor:** Sends HTTP requests, applies retry logic, and records results.
- **RetryStrategy:** Interface for calculating retry delays.
- **RetryComparison:** Runs independent scenarios and compares strategies.

Separating retry execution from retry strategies makes it possible to add new retry algorithms without modifying the core execution loop.

## Testing

Run all tests:

```bash
mvn clean test
```

Run a specific test class:

```bash
mvn -Dtest=RetryExecutorTest test
```

The test suite includes:

- Retry delay calculation tests
- Maximum delay and input validation tests
- HTTP integration tests
- Retry exhaustion and non-retryable response tests
- Per-attempt history verification
- Strategy comparison tests
- Randomized jitter boundary tests

HTTP integration tests use temporary local servers on automatically assigned ports, so manually starting RetryLabServer is unnecessary.

## Implementation Details

- Language: Java 21
- Build tool: Maven
- Networking: Java HttpClient and HttpServer
- Testing: JUnit 5
- Concurrency utility: AtomicInteger
- Timing: System.nanoTime()

RetryLab retries HTTP 503 responses. Other HTTP response codes stop the retry process. Network exceptions are propagated rather than retried.

Execution measurements include HTTP request processing and retry delays.

## License

MIT License. See [LICENSE](LICENSE).
