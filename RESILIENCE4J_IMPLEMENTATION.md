# Resilience4j Circuit Breaker & Retry Implementation

## Overview

This document describes the implementation of Resilience4j patterns (Circuit Breaker, Retry, and Time Limiter) integrated with WebClient for resilient external API calls in the Order Service.

## Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                    Order Service                                 │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────────────┐     ┌──────────────────────────┐  │
│  │  ProductServiceClient    │     │ PaymentServiceClient     │  │
│  │  (WebClient)             │     │ (WebClient)              │  │
│  └───────────┬──────────────┘     └────────────┬─────────────┘  │
│              │                                  │                 │
│              ▼                                  ▼                 │
│  ┌──────────────────────────┐     ┌──────────────────────────┐  │
│  │  Retry Decorator         │     │ Retry Decorator          │  │
│  │  (Max 3 attempts)        │     │ (Max 2 attempts)         │  │
│  └───────────┬──────────────┘     └────────────┬─────────────┘  │
│              │                                  │                 │
│              ▼                                  ▼                 │
│  ┌──────────────────────────┐     ┌──────────────────────────┐  │
│  │ Circuit Breaker          │     │ Circuit Breaker          │  │
│  │ (Failure Rate: 50%)      │     │ (Failure Rate: 70%)      │  │
│  │ (Wait: 30s)              │     │ (Wait: 15s)              │  │
│  └───────────┬──────────────┘     └────────────┬─────────────┘  │
│              │                                  │                 │
│              ▼                                  ▼                 │
│  ┌──────────────────────────┐     ┌──────────────────────────┐  │
│  │ Time Limiter             │     │ Time Limiter             │  │
│  │ (Timeout: 5s)            │     │ (Timeout: 10s)           │  │
│  └───────────┬──────────────┘     └────────────┬─────────────┘  │
│              │                                  │                 │
└──────────────┼──────────────────────────────────┼─────────────────┘
               │                                  │
       ┌───────▼────────┐              ┌────────▼────────┐
       │ Product Service│              │ Payment Service │
       │ (Optional)     │              │ (Optional)      │
       └────────────────┘              └─────────────────┘
```

## Components

### 1. Resilience4jConfig.java
Central configuration class for all resilience patterns.

#### Circuit Breaker Configuration

**Product Service Circuit Breaker:**
- Failure Rate Threshold: 50%
- Slow Call Rate Threshold: 50%
- Slow Call Duration: 2 seconds
- Sliding Window Size: 10 calls
- Minimum Calls to Calculate: 5
- Permitted Calls in Half-Open: 3
- Wait Duration in Open State: 30 seconds
- Automatic OPEN → HALF_OPEN transition: Enabled

**Payment Service Circuit Breaker:**
- Failure Rate Threshold: 70% (more lenient)
- Slow Call Rate Threshold: 50%
- Slow Call Duration: 3 seconds
- Sliding Window Size: 10 calls
- Minimum Calls to Calculate: 5
- Permitted Calls in Half-Open: 3
- Wait Duration in Open State: 15 seconds
- Automatic OPEN → HALF_OPEN transition: Enabled

#### Retry Configuration

**Product Service Retry:**
- Max Attempts: 3
- Wait Duration: 500ms
- Backoff Multiplier: 1.5 (exponential backoff)
- Retry On: Timeout, Connection errors, Server errors
- Don't Retry On: FileNotFoundException

**Payment Service Retry:**
- Max Attempts: 2 (conservative)
- Wait Duration: 1000ms
- Backoff Multiplier: 1.5 (exponential backoff)
- Retry On: Timeout, Connection errors, Server errors
- Don't Retry On: BadRequest

#### Time Limiter Configuration

**Product Service Time Limiter:**
- Timeout: 5 seconds
- Cancel Running Future: true

**Payment Service Time Limiter:**
- Timeout: 10 seconds
- Cancel Running Future: true

### 2. ResilientWebClientHelper.java
Helper class for managing resilience decorator patterns.

Methods:
- `executeWithResilience()` - For reactive calls with full decoration
- `executeWithResilienceBlocking()` - For blocking calls
- `shouldRetry()` - Determines retry eligibility based on exception type

### 3. WebClient Configuration
Separate WebClient beans for each service with custom base URLs.

## Circuit Breaker States

```
CLOSED (Normal Operation)
    │
    ├─── 50% failures detected ───→ OPEN (Service Down)
    │                                  │
    │                                  ├─── After 30s ──→ HALF_OPEN
    │                                  │                    │
    │                                  └── (Test mode)      │
    │                                                        │
    │                          Success ──┬─→ CLOSED
    │                                     │
    │                          Failure ──┴─→ OPEN
    │
    └───────────────────────────────────────────
```

## State Transitions

| Current State | Condition | Next State | Action |
|---------------|-----------|-----------|--------|
| CLOSED | Failure Rate ≥ 50% | OPEN | Block calls, log error |
| CLOSED | Normal | CLOSED | Allow calls |
| OPEN | Wait Duration elapsed | HALF_OPEN | Allow limited test calls |
| HALF_OPEN | Success (3 calls) | CLOSED | Resume normal operation |
| HALF_OPEN | Failure | OPEN | Return to blocking state |

## Retry Mechanism

### Exponential Backoff Formula
```
waitDuration = initialWaitDuration * (multiplier ^ (attempt - 1))
```

### Product Service Retry Attempts
```
Attempt 1: Immediate
Attempt 2: After 500ms
Attempt 3: After 500ms * 1.5 = 750ms
Total: 1250ms maximum retry time
```

### Payment Service Retry Attempts
```
Attempt 1: Immediate
Attempt 2: After 1000ms
Total: 1000ms maximum retry time
```

## Integration with ProductServiceClient

```java
// WebClient call with full resilience decoration
return Retry.decorateFunction(productServiceRetry, (id) -> {
    return productServiceWebClient
        .get()
        .uri("/products/" + id)
        .retrieve()
        .bodyToMono(ProductDTO.class)
        .timeout(productServiceTimeLimiter.getTimeLimiterConfig().getTimeoutDuration())
        .doOnError(e -> productServiceCircuitBreaker.onError(0, e))
        .doOnSuccess(result -> productServiceCircuitBreaker.onSuccess(0))
        .block();
}).apply(productId);
```

## Integration with PaymentServiceClient

```java
// WebClient POST call with full resilience decoration
return Retry.decorateFunction(paymentServiceRetry, (request) -> {
    return paymentServiceWebClient
        .post()
        .uri("/payments/process")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .bodyToMono(PaymentResponseDTO.class)
        .timeout(paymentServiceTimeLimiter.getTimeLimiterConfig().getTimeoutDuration())
        .doOnError(e -> paymentServiceCircuitBreaker.onError(0, e))
        .doOnSuccess(result -> paymentServiceCircuitBreaker.onSuccess(0))
        .block();
}).apply(paymentRequest);
```

## Exception Handling

### Handled Exceptions

| Exception Type | Should Retry | Circuit Breaker Impact |
|---|---|---|
| TimeoutException | Yes | Records as error |
| ConnectException | Yes | Records as error |
| SocketTimeoutException | Yes | Records as error |
| HttpServerErrorException (5xx) | Yes | Records as error |
| HttpClientErrorException (4xx) | No | Records as error |
| FileNotFoundException | No | Ignored |
| CircuitBreakerOpenException | No | Already open |

### Exception Flow

```
WebClient Call
    │
    ├─ Success (2xx/3xx) ────→ Return result
    │                            Record success in CB
    │
    └─ Error Occurs
       │
       ├─ TimeoutException ──────→ Retry (if attempts left)
       │
       ├─ ConnectException ──────→ Retry (if attempts left)
       │
       ├─ 5xx Server Error ──────→ Retry (if attempts left)
       │
       ├─ 4xx Client Error ──────→ Don't retry
       │                           Throw exception
       │
       └─ Other Error ───────────→ Throw exception
```

## Monitoring & Health Checks

### Actuator Endpoints

```
# Circuit Breaker Health
GET /api/actuator/health

# Circuit Breaker Details
GET /api/actuator/circuitbreakers

# Retry Details
GET /api/actuator/retries

# Metrics
GET /api/actuator/metrics
```

### Health Response Example

```json
{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "productServiceCircuitBreaker": {
          "status": "UP",
          "details": {
            "state": "CLOSED",
            "failureRate": "0.0%",
            "slowCallRate": "0.0%",
            "bufferedCalls": 5,
            "failedCalls": 0,
            "slowCalls": 0,
            "slowFailedCalls": 0,
            "successfulCalls": 5
          }
        }
      }
    }
  }
}
```

## Configuration Properties

All configurations can be overridden in `application.properties`:

```properties
# Circuit Breaker - Product Service
resilience4j.circuitbreaker.instances.productServiceCircuitBreaker.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.productServiceCircuitBreaker.wait-duration-in-open-state=30s

# Retry - Product Service
resilience4j.retry.instances.productServiceRetry.max-attempts=3
resilience4j.retry.instances.productServiceRetry.wait-duration=500ms

# Time Limiter - Product Service
resilience4j.timelimiter.instances.productServiceTimeLimiter.timeout-duration=5s
```

## Usage Examples

### Example 1: Normal Operation
```
Request → WebClient → Success (2xx) → Circuit Breaker: CLOSED → Return result
```

### Example 2: Service Timeout (First 2 Attempts)
```
Request → WebClient → Timeout
       ↓
Retry 1 → Timeout
       ↓
Retry 2 → Success (2xx) → Circuit Breaker: CLOSED → Return result
```

### Example 3: Circuit Breaker Opens
```
Request 1 → Timeout → CB records error
Request 2 → Timeout → CB records error
Request 3 → Timeout → CB records error
Request 4 → Timeout → CB records error
Request 5 → Timeout → CB records error
        (5+ calls with 50% failure rate triggers OPEN)
       ↓
Circuit Breaker: OPEN
       ↓
Request 6 → Blocked immediately → CircuitBreakerOpenException
                                   (No retry attempts)
                                   
After 30 seconds:
Circuit Breaker: HALF_OPEN (testing mode)
       ↓
Request 7 → Allowed (1/3 test calls) → Success → Circuit Breaker: CLOSED
```

### Example 4: Circuit Breaker Half-Open Fails
```
Circuit Breaker: HALF_OPEN (testing mode)
       ↓
Request 7 → Allowed (1/3 test calls) → Timeout → Circuit Breaker: OPEN
(No more test calls allowed, back to blocking)
```

## Logging

### Log Levels

- **DEBUG**: Detailed operation information
  ```
  Calling Product Service for Product ID: 1 (Attempt)
  Product Service call succeeded
  ```

- **INFO**: Important events
  ```
  Fetching product availability for Product ID: 1 (Circuit Breaker State: CLOSED)
  Successfully fetched product from Product Service
  ```

- **WARN**: Warnings and recoverable issues
  ```
  Retrying Product Service due to timeout/connection error
  Circuit Breaker for Product Service state changed from CLOSED to OPEN
  ```

- **ERROR**: Error conditions
  ```
  ERROR - Failed to connect to Product Service. Service might be down
  ERROR - Circuit Breaker for Product Service is OPEN
  ```

## Best Practices

1. **Understand Your Service Dependencies**
   - Know which services are critical
   - Set appropriate timeout values

2. **Configure Failure Thresholds Carefully**
   - Too aggressive (low threshold) → Circuit opens too quickly
   - Too lenient (high threshold) → Slow to detect failures

3. **Monitor Circuit Breaker States**
   - Check actuator endpoints regularly
   - Set up alerts for state changes

4. **Test Failure Scenarios**
   - Simulate service downtime
   - Verify fallback behavior

5. **Use Appropriate Timeouts**
   - Product Service: 5s (faster service expected)
   - Payment Service: 10s (possibly slower, external API)

6. **Exponential Backoff**
   - Reduces load on struggling services
   - Increases chance of recovery

## Troubleshooting

### Problem: Circuit Breaker Stuck in OPEN
**Solution:**
```
1. Check if service is actually down
2. Wait for wait-duration-in-open-state (30s for Product, 15s for Payment)
3. Service will automatically transition to HALF_OPEN
4. Or restart the Order Service to reset state
```

### Problem: Retry Not Working
**Solution:**
```
1. Check if exception is in retry-exceptions list
2. Verify max-attempts > 0
3. Check logs for "ignore-exceptions" - might be on ignore list
4. Verify wait-duration is configured
```

### Problem: Time Limiter Not Enforcing Timeout
**Solution:**
```
1. Verify timeout-duration is configured correctly
2. Check if .timeout() is called on Mono
3. Ensure cancel-running-future=true
```

## Performance Implications

### Latency Impact
```
Scenario 1: All services up
- Additional latency: <5ms (decoration overhead)

Scenario 2: First attempt fails, retry succeeds
- Product Service: +500ms to +1250ms
- Payment Service: +1000ms

Scenario 3: Circuit breaker OPEN
- Latency: <1ms (immediate failure)
- Protects downstream from cascading failures
```

### Resource Usage
```
- Circuit Breaker: Minimal (in-memory state tracking)
- Retry: Minimal (reuses same connection)
- Time Limiter: Low (scheduling overhead)
- WebClient: Efficient (reactive, non-blocking)
```

## Future Enhancements

- [ ] Bulkhead pattern for thread isolation
- [ ] Fallback strategies with alternative endpoints
- [ ] Custom metrics and dashboards
- [ ] Request/Response caching
- [ ] Dynamic configuration updates
- [ ] Rate limiting integration
- [ ] Distributed tracing integration

## References

- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Spring WebClient](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-client)
- [Circuit Breaker Pattern](https://martinfowler.com/bliki/CircuitBreaker.html)

