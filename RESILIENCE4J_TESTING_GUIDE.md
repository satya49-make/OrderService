# Resilience4j Testing Guide

## Testing Circuit Breaker and Retry Patterns

This guide provides step-by-step instructions to test the Resilience4j implementation in the Order Service.

## Prerequisites

1. Order Service running on `http://localhost:8080`
2. MySQL database configured
3. Sample products in database

## Test Scenarios

### Scenario 1: Normal Operation (Circuit Breaker CLOSED)

**Setup:**
- Both Product Service and Payment Service are running
- All services respond normally

**Test Steps:**

```bash
# 1. Create a product (if not exists)
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "Test Laptop",
    "description": "Test Product",
    "price": 999.99,
    "availableQuantity": 100
  }'

# 2. Check circuit breaker status
curl http://localhost:8080/api/actuator/circuitbreakers

# Expected Response:
# {
#   "circuitBreakers": [
#     {
#       "name": "productServiceCircuitBreaker",
#       "state": "CLOSED",
#       "metrics": {...}
#     }
#   ]
# }

# 3. Create multiple orders
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d "{
      \"productId\": 1,
      \"quantity\": 1,
      \"customerName\": \"Customer $i\",
      \"customerEmail\": \"customer$i@test.com\",
      \"shippingAddress\": \"Test Address $i\",
      \"paymentMethod\": \"CREDIT_CARD\"
    }"
  sleep 1
done

# 4. Check logs for successful calls
# Expected: INFO - Successfully fetched product from Product Service
```

**Expected Results:**
- ✅ All orders created successfully
- ✅ Circuit Breaker state: CLOSED
- ✅ No retries needed
- ✅ Log shows "Successfully fetched product"

---

### Scenario 2: Simulate Service Timeout (Retry Mechanism)

**Setup:**
- Slow Product Service (add delay via middleware or mock)
- Circuit Breaker should remain CLOSED

**Test Steps:**

```bash
# Assume you have a way to slow down the Product Service
# (e.g., mock service with artificial delay)

# 1. Add delay to Product Service responses
# Option A: Use a proxy/middleware to add 1 second delay
# Option B: Modify mock service to sleep

# 2. Create order - should retry on timeout
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 1,
    "customerName": "Test User",
    "customerEmail": "test@test.com",
    "shippingAddress": "Test Address",
    "paymentMethod": "CREDIT_CARD"
  }'

# 3. Check logs for retry information
# Expected:
# WARN - Retrying Product Service due to timeout/connection error: Attempt 1 of 3
# WARN - Retrying Product Service due to timeout/connection error: Attempt 2 of 3
# INFO - Successfully fetched product from Product Service (after retry)
```

**Expected Results:**
- ✅ Request succeeds after retry
- ✅ Logs show multiple attempts
- ✅ Circuit Breaker remains CLOSED
- ✅ Total latency increased by retry wait duration

---

### Scenario 3: Circuit Breaker Opens (Service Down)

**Setup:**
- Stop Product Service completely
- Make multiple requests to trigger circuit breaker opening

**Test Steps:**

```bash
# 1. Stop Product Service
# docker stop product-service
# OR kill the service process

# 2. Check circuit breaker status before requests
curl http://localhost:8080/api/actuator/circuitbreakers

# Expected: "state": "CLOSED"

# 3. Make 5+ requests to trigger circuit breaker
for i in {1..6}; do
  echo "Request $i"
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d "{
      \"productId\": 1,
      \"quantity\": 1,
      \"customerName\": \"Customer $i\",
      \"customerEmail\": \"customer$i@test.com\",
      \"shippingAddress\": \"Address $i\",
      \"paymentMethod\": \"CREDIT_CARD\"
    }" 2>/dev/null | jq .
  sleep 1
done

# 4. Check circuit breaker status after failures
curl http://localhost:8080/api/actuator/circuitbreakers | jq '.circuitBreakers[0].state'

# Expected: "state": "OPEN"

# 5. Try to make another order
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 1,
    "customerName": "Final Test",
    "customerEmail": "final@test.com",
    "shippingAddress": "Final Address",
    "paymentMethod": "CREDIT_CARD"
  }'

# Expected: 400 Bad Request
# Message: "Product Service is temporarily unavailable due to multiple failures"
```

**Expected Results:**
- ✅ First 5 requests fail with timeout errors
- ✅ Circuit Breaker state changes from CLOSED to OPEN
- ✅ 6th request fails immediately without retrying
- ✅ Error message indicates circuit breaker is OPEN
- ✅ Logs show: "Circuit Breaker state changed from CLOSED to OPEN"

---

### Scenario 4: Circuit Breaker Half-Open (Recovery Testing)

**Setup:**
- Circuit Breaker is currently OPEN
- Wait for automatic transition to HALF_OPEN (30 seconds for Product Service)

**Test Steps:**

```bash
# After Scenario 3, circuit breaker is OPEN
# Wait 30 seconds for automatic transition to HALF_OPEN
echo "Waiting 30 seconds for HALF_OPEN state..."
sleep 30

# 1. Check circuit breaker status
curl http://localhost:8080/api/actuator/circuitbreakers | jq '.circuitBreakers[0].state'

# Expected: "state": "HALF_OPEN"

# 2. Start Product Service again
# docker start product-service
# OR restart the service

# 3. Make request during HALF_OPEN (test call)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 1,
    "customerName": "Recovery Test",
    "customerEmail": "recovery@test.com",
    "shippingAddress": "Recovery Address",
    "paymentMethod": "CREDIT_CARD"
  }'

# Expected: 201 Created (if service is back up)
# Circuit Breaker should transition to CLOSED after successful test call

# 4. Verify circuit breaker is CLOSED again
curl http://localhost:8080/api/actuator/circuitbreakers | jq '.circuitBreakers[0].state'

# Expected: "state": "CLOSED"

# 5. Make another request to confirm normal operation
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 1,
    "customerName": "Final Confirmation",
    "customerEmail": "confirm@test.com",
    "shippingAddress": "Confirm Address",
    "paymentMethod": "CREDIT_CARD"
  }'

# Expected: 201 Created
```

**Expected Results:**
- ✅ Circuit Breaker automatically transitions to HALF_OPEN after 30 seconds
- ✅ Test call succeeds if service is running
- ✅ Circuit Breaker transitions back to CLOSED
- ✅ Normal operations resume
- ✅ Log shows state transitions

---

### Scenario 5: Payment Service Circuit Breaker (More Lenient)

**Setup:**
- Stop Payment Service
- Set `payment.service.enabled=true` in application.properties
- Payment Service has 70% failure threshold (more lenient than Product Service)

**Test Steps:**

```bash
# 1. Update application.properties
# Change: payment.service.enabled=false to payment.service.enabled=true
# Restart Order Service

# 2. Stop Payment Service
# docker stop payment-service

# 3. Make 7-8 requests to trigger Payment Service circuit breaker
for i in {1..8}; do
  echo "Payment Test Request $i"
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d "{
      \"productId\": 1,
      \"quantity\": 1,
      \"customerName\": \"Payment Test $i\",
      \"customerEmail\": \"payment$i@test.com\",
      \"shippingAddress\": \"Payment Address $i\",
      \"paymentMethod\": \"CREDIT_CARD\"
    }" 2>/dev/null | jq .
  sleep 1
done

# 4. Check Payment Service circuit breaker status
curl http://localhost:8080/api/actuator/circuitbreakers | \
  jq '.circuitBreakers[] | select(.name=="paymentServiceCircuitBreaker")'

# Expected: "state": "OPEN" after 70% failure rate

# 5. Verify fallback to local payment processing
# When circuit breaker opens, local payment processing should be used
```

**Expected Results:**
- ✅ Payment Service circuit breaker opens after ~7 failures
- ✅ Orders still created successfully (fallback to local processing)
- ✅ Payment status shows "Local Processing"
- ✅ Payment Service circuit breaker more lenient than Product Service

---

### Scenario 6: Concurrent Requests Under Load

**Setup:**
- Simulate multiple concurrent requests
- Measure resilience and circuit breaker behavior

**Test Steps:**

```bash
# 1. Create 20 concurrent requests
echo "Starting 20 concurrent requests..."
for i in {1..20}; do
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d "{
      \"productId\": 1,
      \"quantity\": 1,
      \"customerName\": \"Concurrent $i\",
      \"customerEmail\": \"concurrent$i@test.com\",
      \"shippingAddress\": \"Concurrent Address $i\",
      \"paymentMethod\": \"CREDIT_CARD\"
    }" &
done
wait

# 2. Check metrics
curl http://localhost:8080/api/actuator/metrics/resilience4j.circuitbreaker.calls | jq .

# 3. Verify no cascading failures
curl http://localhost:8080/api/actuator/circuitbreakers
```

**Expected Results:**
- ✅ Multiple concurrent requests handled properly
- ✅ Circuit breaker protects against cascading failures
- ✅ Appropriate metrics recorded
- ✅ Response times reasonable

---

## Monitoring Circuit Breaker

### Health Endpoint

```bash
# Check overall health
curl http://localhost:8080/api/actuator/health | jq .

# Sample Response:
# {
#   "status": "UP",
#   "components": {
#     "circuitBreakers": {
#       "status": "UP",
#       "details": {
#         "productServiceCircuitBreaker": {
#           "state": "CLOSED",
#           "failureRate": "0.0%",
#           "slowCallRate": "0.0%"
#         }
#       }
#     }
#   }
# }
```

### Detailed Metrics

```bash
# Get all circuit breaker details
curl http://localhost:8080/api/actuator/circuitbreakers | jq .

# Get circuit breaker metrics
curl http://localhost:8080/api/actuator/metrics | jq '.names[] | select(contains("resilience4j"))'

# Get specific metric
curl "http://localhost:8080/api/actuator/metrics/resilience4j.circuitbreaker.state?tag=name:productServiceCircuitBreaker"
```

---

## Log Examination

### Filter logs by service

```bash
# View Product Service circuit breaker logs
grep "productServiceCircuitBreaker" application.log

# View Payment Service circuit breaker logs
grep "paymentServiceCircuitBreaker" application.log

# View retry logs
grep "Retry" application.log

# View circuit breaker state changes
grep "Circuit Breaker" application.log | grep "state changed"
```

### Key log messages

```
# Successful operation
INFO - Successfully fetched product from Product Service

# Retry attempt
WARN - Retrying Product Service due to timeout/connection error: Attempt 1 of 3

# Circuit breaker state change
WARN - Circuit Breaker productServiceCircuitBreaker state changed from CLOSED to OPEN

# Circuit breaker blocked call
ERROR - Circuit Breaker for Product Service is OPEN. Service is temporarily unavailable.

# Half-open test
INFO - Circuit Breaker productServiceCircuitBreaker state changed from OPEN to HALF_OPEN
```

---

## Debugging Circuit Breaker Issues

### Circuit Breaker Stuck in OPEN

**Problem:** Circuit breaker is stuck in OPEN state even after service recovery

**Solution:**
```bash
# Option 1: Wait for automatic transition
# Wait duration is 30s for Product Service, 15s for Payment Service

# Option 2: Reset manually (restart application)
# This clears all in-memory circuit breaker states

# Option 3: Reduce wait-duration in application.properties
resilience4j.circuitbreaker.instances.productServiceCircuitBreaker.wait-duration-in-open-state=10s

# Option 4: Check if service is really up
curl http://localhost:8081/api/products/1  # Product Service
curl http://localhost:8082/api/payments  # Payment Service
```

### Retry Not Working

**Problem:** Requests fail immediately without retrying

**Solution:**
```bash
# 1. Check if exception is in retry-exceptions list
grep "retry-exceptions" application.properties

# 2. Verify max-attempts is > 1
grep "max-attempts" application.properties

# 3. Check if exception is in ignore-exceptions list
grep "ignore-exceptions" application.properties

# 4. Enable DEBUG logging to see retry attempts
# In application.properties:
logging.level.com.order=DEBUG

# 5. Check logs for retry messages
tail -f application.log | grep "Retry"
```

### Time Limiter Not Enforcing Timeout

**Problem:** Requests that should timeout are not being canceled

**Solution:**
```bash
# 1. Verify timeout-duration is set correctly
grep "timeout-duration" application.properties

# 2. Check if cancel-running-future is true
grep "cancel-running-future" application.properties

# 3. Verify .timeout() is called on WebClient Mono
# Check ProductServiceClient.java and PaymentServiceClient.java

# 4. Increase timeout for debugging
resilience4j.timelimiter.instances.productServiceTimeLimiter.timeout-duration=30s
```

---

## Performance Testing

### Load Test Script

```bash
#!/bin/bash
# performance_test.sh

echo "Starting performance test..."
START_TIME=$(date +%s%N)

# Make 100 requests
for i in {1..100}; do
  curl -X GET http://localhost:8080/api/products/1 \
    -H "Content-Type: application/json" \
    -s > /dev/null &
done

wait

END_TIME=$(date +%s%N)
DURATION=$((($END_TIME - $START_TIME) / 1000000))

echo "Test completed in ${DURATION}ms"
echo "Average: $((DURATION / 100))ms per request"
```

---

## Cleanup After Testing

```bash
# Clear circuit breaker states
curl -X POST http://localhost:8080/api/actuator/circuitbreakers/productServiceCircuitBreaker/reset

# Remove test orders
DELETE FROM orders WHERE customer_email LIKE '%test%';

# Remove test products
DELETE FROM products WHERE product_name LIKE '%Test%';

# Restart application for clean state
```

---

## Best Testing Practices

1. **Test in isolation**: Test one scenario at a time
2. **Document results**: Note state transitions and timings
3. **Monitor logs**: Always check logs for detailed information
4. **Verify metrics**: Use actuator endpoints to confirm state
5. **Test recovery**: Ensure services recover properly after failures
6. **Load test**: Test with multiple concurrent requests
7. **Measure latency**: Understand impact of retries and timeouts
8. **Clean up**: Remove test data after testing

