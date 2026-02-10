# Quick Start Guide - Resilience4j Integration

## 5-Minute Setup Guide

### Step 1: Build the Project
```bash
cd C:\project2\OrderService
.\gradlew.bat clean build
```

### Step 2: Run the Application
```bash
.\gradlew.bat bootRun
```

Expected output:
```
Order Service
Configuring Circuit Breaker for Product Service
Configuring Circuit Breaker for Payment Service
Configuring Retry for Product Service
Configuring Retry for Payment Service
Configuring TimeLimiter for Product Service
Configuring TimeLimiter for Payment Service
```

### Step 3: Verify It's Running
```bash
curl http://localhost:8080/api/actuator/health
```

Expected response: `{"status":"UP"}`

### Step 4: Check Circuit Breaker Status
```bash
curl http://localhost:8080/api/actuator/circuitbreakers | jq .
```

## Key Features

### 🛡️ Circuit Breaker Protection
- **Product Service**: Opens at 50% failure rate (30s recovery)
- **Payment Service**: Opens at 70% failure rate (15s recovery)
- Prevents cascading failures by blocking calls when service is down

### 🔄 Automatic Retry
- **Product Service**: Up to 3 attempts with exponential backoff (500ms base)
- **Payment Service**: Up to 2 attempts with exponential backoff (1000ms base)
- Handles transient failures automatically

### ⏱️ Request Timeout
- **Product Service**: 5-second timeout
- **Payment Service**: 10-second timeout
- Prevents hanging requests

### 📊 Full Observability
- Health endpoints
- Circuit breaker metrics
- Retry statistics
- Comprehensive logging

## Testing

### Test 1: Normal Operation (✅ Success Expected)
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 1,
    "customerName": "Test User",
    "customerEmail": "test@example.com",
    "shippingAddress": "Test Address",
    "paymentMethod": "CREDIT_CARD"
  }'
```

Check logs:
```
INFO - Successfully fetched product from Product Service
INFO - Order created successfully
```

### Test 2: Check Circuit Breaker Status (All Services Up)
```bash
curl http://localhost:8080/api/actuator/circuitbreakers | \
  jq '.circuitBreakers[] | {name, state: .details.state}'
```

Expected:
```json
{
  "name": "productServiceCircuitBreaker",
  "state": "CLOSED"
}
```

### Test 3: Simulate Service Down (⚠️ Expected to Fail)
1. Stop Product Service
2. Try to create order:
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 1,
    "customerName": "Test",
    "customerEmail": "test@example.com",
    "shippingAddress": "Test",
    "paymentMethod": "CREDIT_CARD"
  }'
```

Check logs for:
```
WARN - Retrying Product Service due to timeout/connection error: Attempt 1 of 3
WARN - Retrying Product Service due to timeout/connection error: Attempt 2 of 3
WARN - Retrying Product Service due to timeout/connection error: Attempt 3 of 3
ERROR - Circuit Breaker for Product Service is OPEN
```

After ~5 more failures, circuit breaker opens:
```
WARN - Circuit Breaker productServiceCircuitBreaker state changed from CLOSED to OPEN
```

### Test 4: Monitor Circuit Breaker Recovery
After circuit breaker is OPEN, wait 30 seconds:
```bash
# Check status
curl http://localhost:8080/api/actuator/circuitbreakers | \
  jq '.circuitBreakers[0].details | {state, failureRate}'

# Should show: "state": "HALF_OPEN"

# Start Product Service again
# Make a request - it will be tested in HALF_OPEN state

# After successful request:
# "state": "CLOSED"
```

## Configuration

### Location
`src/main/resources/application.properties`

### Key Settings

```properties
# Circuit Breaker - Product Service
resilience4j.circuitbreaker.instances.productServiceCircuitBreaker.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.productServiceCircuitBreaker.wait-duration-in-open-state=30s

# Circuit Breaker - Payment Service
resilience4j.circuitbreaker.instances.paymentServiceCircuitBreaker.failure-rate-threshold=70
resilience4j.circuitbreaker.instances.paymentServiceCircuitBreaker.wait-duration-in-open-state=15s

# Retry - Product Service
resilience4j.retry.instances.productServiceRetry.max-attempts=3
resilience4j.retry.instances.productServiceRetry.wait-duration=500ms

# Retry - Payment Service
resilience4j.retry.instances.paymentServiceRetry.max-attempts=2
resilience4j.retry.instances.paymentServiceRetry.wait-duration=1000ms
```

### How to Customize

**Make circuit breaker more aggressive (open faster):**
```properties
resilience4j.circuitbreaker.instances.productServiceCircuitBreaker.failure-rate-threshold=30
```

**Increase retry attempts:**
```properties
resilience4j.retry.instances.productServiceRetry.max-attempts=5
```

**Extend timeout:**
```properties
resilience4j.timelimiter.instances.productServiceTimeLimiter.timeout-duration=10s
```

## Monitoring

### Health Dashboard
```bash
curl http://localhost:8080/api/actuator/health | jq .components.circuitBreakers
```

### Metrics
```bash
# All metrics
curl http://localhost:8080/api/actuator/metrics | jq .names

# Specific circuit breaker metric
curl "http://localhost:8080/api/actuator/metrics/resilience4j.circuitbreaker.calls?tag=name:productServiceCircuitBreaker"
```

### Log Monitoring
```bash
# View all resilience-related logs
tail -f application.log | grep -E "Circuit Breaker|Retry|resilience4j"

# View specific service
tail -f application.log | grep "productServiceCircuitBreaker"
```

## Troubleshooting

### Circuit Breaker Stuck in OPEN?
```bash
# Option 1: Wait 30 seconds (automatic recovery)
sleep 30

# Option 2: Restart application
.\gradlew.bat bootRun

# Option 3: Check if service is actually down
curl http://localhost:8081/api/products/1  # Product Service endpoint
```

### Retries Not Working?
```bash
# Enable DEBUG logging
# In application.properties: logging.level.com.order=DEBUG

# Check logs for retry messages
grep "Retry" application.log

# Verify max-attempts is configured
grep "max-attempts" application.properties
```

### Want Different Behavior?
```
1. Identify the setting in application.properties
2. Change the value
3. Restart the application (changes take effect)
4. Verify with actuator endpoint
```

## Important Files

| File | Purpose |
|------|---------|
| `Resilience4jConfig.java` | Main configuration |
| `ResilientWebClientHelper.java` | Helper utilities |
| `ProductServiceClient.java` | Product API calls with resilience |
| `PaymentServiceClient.java` | Payment API calls with resilience |
| `application.properties` | Configuration settings |

## Documentation

- 📖 **RESILIENCE4J_IMPLEMENTATION.md** - Technical deep dive
- 🧪 **RESILIENCE4J_TESTING_GUIDE.md** - Detailed test scenarios
- 📋 **RESILIENCE4J_SUMMARY.md** - Complete implementation summary
- ❌ **ERROR_HANDLING_DOCUMENTATION.md** - Error handling & service failures

## What Happens When Services Go Down

### Scenario 1: Product Service Unreachable

```
User Request → Order Creation
       ↓
ProductServiceClient tries to fetch product
       ↓
Network timeout
       ↓
RETRY ATTEMPT 1 (immediately)
       ↓
Still timeout
       ↓
RETRY ATTEMPT 2 (after 500ms)
       ↓
Still timeout
       ↓
RETRY ATTEMPT 3 (after 750ms)
       ↓
All retries failed
       ↓
Circuit Breaker records error
       ↓
(After 5+ errors) Circuit Breaker opens
       ↓
Subsequent requests fail immediately (without retry)
       ↓
Response: 400 Bad Request - "Product Service is temporarily unavailable"
```

### Scenario 2: Payment Service Unreachable (with fallback)

```
Order Creation with payment
       ↓
PaymentServiceClient tries to call Payment Service
       ↓
Network timeout
       ↓
RETRY ATTEMPT 1 (immediately)
       ↓
Still timeout
       ↓
RETRY ATTEMPT 2 (after 1000ms)
       ↓
All retries failed
       ↓
Catch exception in PaymentService
       ↓
FALLBACK to LOCAL payment processing
       ↓
Local payment succeeds (95% success rate)
       ↓
Order created successfully with local payment
       ↓
Response: 201 Created - Order successful
```

## Performance Impact

### No Service Issues
- Resilience4j adds **<5ms** latency

### Service Timeout + Retry Success
- Product Service: **+500ms to +1250ms** (depends on retry)
- Payment Service: **+1000ms** (depends on retry)

### Circuit Breaker Open
- Latency: **<1ms** (immediate failure)
- Protects system from overload

## Real-World Use Cases

### Case 1: Network Hiccup
```
Request 1: Timeout → Retry → Success ✅
No circuit breaker opened, user gets order
```

### Case 2: Service Restart
```
Requests 1-5: Timeout → Retry → Timeout
Circuit breaker opens after 5 failures
Wait 30 seconds for service restart
Circuit breaker auto-transitions to HALF_OPEN
Request 6: Test call → Success → Circuit breaker closes ✅
Service recovered, normal operation resumes
```

### Case 3: Complete Service Failure
```
Requests 1-5: All fail → Circuit breaker opens
Requests 6+: Fail immediately (no retry) → Fast response time
Circuit breaker protects system from being overwhelmed
```

## Next Steps

1. ✅ Build and run the application
2. ✅ Test normal operation
3. ✅ Simulate service failures
4. ✅ Monitor metrics and logs
5. ✅ Tune configuration for your needs
6. ✅ Deploy to production

## Support

For detailed information, refer to:
- Configuration details → `RESILIENCE4J_IMPLEMENTATION.md`
- Testing scenarios → `RESILIENCE4J_TESTING_GUIDE.md`
- Error handling → `ERROR_HANDLING_DOCUMENTATION.md`

