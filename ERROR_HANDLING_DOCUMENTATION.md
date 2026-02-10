# Error Handling & Service Resilience Documentation

## Overview

This document describes how the Order Service handles errors when external services (Product Service and Payment Service) are down, unavailable, or throw exceptions. The system includes comprehensive logging, graceful degradation, and fallback mechanisms.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Order Service                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐ │
│  │ Order Controller │  │Product Controller│  │ Exception    │ │
│  │                  │  │                  │  │ Handler      │ │
│  └────────┬─────────┘  └────────┬─────────┘  └──────────────┘ │
│           │                     │                     ▲         │
│           └─────────┬───────────┴─────────┬──────────┘         │
│                     │                     │                     │
│  ┌──────────────────▼──────┐  ┌──────────▼────────────┐       │
│  │   Order Service         │  │  Payment Service      │       │
│  │                         │  │                       │       │
│  │ • checkProduct()   ──┐  │  │ • processPayment()    │       │
│  │ • updateQuantity()   │  │  │ • handleExternal()    │       │
│  │ • handleErrors() ◄───┘  │  │ • fallbackLocal()     │       │
│  └────────┬────────────────┘  └──────────┬────────────┘       │
│           │                             │                     │
└───────────┼─────────────────────────────┼─────────────────────┘
            │                             │
    ┌───────▼────────┐          ┌────────▼────────┐
    │ Product        │          │ Payment         │
    │ Service        │          │ Service         │
    │ (Optional)     │          │ (Optional)      │
    └────────────────┘          └─────────────────┘
       OR
    ┌──────────────────────────────────────────┐
    │ Local Database                           │
    │ (Fallback - Always Available)            │
    └──────────────────────────────────────────┘
```

## Error Handling Strategy

### 1. Product Service Failures

#### Scenarios Handled

| Scenario | HTTP Status | Behavior | Logging |
|----------|------------|----------|---------|
| Service Unavailable (503) | 503 | Throw ProductNotAvailableException | ERROR - "Product Service is unavailable" |
| Service Timeout | N/A | Throw ProductNotAvailableException | ERROR - "Failed to connect to Product Service" |
| Connection Refused | N/A | Throw ProductNotAvailableException | ERROR - "Service might be down or unreachable" |
| Product Not Found (404) | 404 | Throw ProductNotAvailableException | WARN - "Product not found in Product Service" |
| Server Error (5xx) | 5xx | Throw ProductNotAvailableException | ERROR - "Product Service encountered an error" |
| Invalid Request (4xx) | 4xx | Throw ProductNotAvailableException | ERROR - "Invalid request to Product Service" |
| Null Response | N/A | Throw ProductNotAvailableException | WARN - "Product API returned null" |

#### Code Location
- **Client**: `com.order.client.ProductServiceClient`
- **Error Handling**: Comprehensive try-catch with specific exception handling for each HTTP status

#### Example Log Output
```
2026-02-10 15:30:45 ERROR [http-nio-8080-exec-1] com.order.client.ProductServiceClient - Failed to connect to Product Service. Service might be down or unreachable. URL: http://localhost:8081/api/products, Error: Connection refused

2026-02-10 15:30:46 ERROR [http-nio-8080-exec-1] com.order.service.OrderService - Product availability check failed for Product ID: 1. Error: Cannot reach Product Service. Please check if the service is running or try again later.

2026-02-10 15:30:46 ERROR [http-nio-8080-exec-2] com.order.exception.GlobalExceptionHandler - CRITICAL: Product Service is down or unreachable. Error: Cannot reach Product Service. Please check if the service is running or try again later.
```

### 2. Payment Service Failures

#### Scenarios Handled

| Scenario | HTTP Status | Behavior | Logging |
|----------|------------|----------|---------|
| Service Unavailable (503) | 503 | Log ERROR, Fallback to local processing | ERROR - "Payment Service is unavailable" |
| Gateway Timeout (504) | 504 | Log ERROR, Fallback to local processing | ERROR - "Payment Service gateway timeout" |
| Service Timeout | N/A | Log ERROR, Fallback to local processing | ERROR - "Request took too long to process" |
| Connection Refused | N/A | Log ERROR, Fallback to local processing | ERROR - "Cannot reach Payment Service" |
| Server Error (5xx) | 5xx | Log ERROR, Fallback to local processing | ERROR - "Payment Service encountered an error" |
| Invalid Request (4xx) | 4xx | Throw PaymentFailedException | ERROR - "Invalid payment details" |
| Null Response | N/A | Throw PaymentFailedException | WARN - "Payment Service returned null" |

#### Code Location
- **Client**: `com.order.client.PaymentServiceClient`
- **Service**: `com.order.service.PaymentService`
- **Error Handling**: Fallback to local payment processing when external service fails

#### Example Log Output
```
2026-02-10 15:35:20 INFO [http-nio-8080-exec-3] com.order.service.PaymentService - Processing payment for amount: 1999.98, Method: CREDIT_CARD

2026-02-10 15:35:20 INFO [http-nio-8080-exec-3] com.order.service.PaymentService - Attempting to process payment with external Payment Service. Transaction ID: 550e8400-e29b-41d4-a716

2026-02-10 15:35:25 ERROR [http-nio-8080-exec-3] com.order.client.PaymentServiceClient - Failed to connect to Payment Service. Service might be down or unreachable. URL: http://localhost:8082/api/payments, Error: Connection refused

2026-02-10 15:35:25 WARN [http-nio-8080-exec-3] com.order.service.PaymentService - External payment service failed. Attempting fallback to local payment processing. Error: Cannot reach Payment Service. The service might be offline. Please try again later.

2026-02-10 15:35:25 INFO [http-nio-8080-exec-3] com.order.service.PaymentService - Processing payment locally (fallback/local mode). Transaction ID: 550e8400-e29b-41d4-a716

2026-02-10 15:35:26 INFO [http-nio-8080-exec-3] com.order.service.PaymentService - Local payment processing successful. Transaction ID: 550e8400-e29b-41d4-a716

2026-02-10 15:35:26 INFO [http-nio-8080-exec-3] com.order.service.PaymentService - Local payment saved successfully. Payment ID: 1, Status: SUCCESS
```

### 3. Global Exception Handler

#### Responsibilities
- Catches all exceptions not handled by specific handlers
- Detects service connectivity issues
- Logs exceptions with appropriate severity levels (CRITICAL, ERROR, WARN)
- Returns user-friendly error messages
- Maps exceptions to appropriate HTTP status codes

#### Exception Types Handled

```
ProductNotAvailableException
├── Bad Request (400) - Product not found or insufficient stock
└── Logged with ERROR/WARN level

PaymentFailedException
├── Payment Required (402) - Payment processing failed
└── Logged with CRITICAL/ERROR/WARN level

OrderNotFoundException
├── Not Found (404) - Order doesn't exist
└── Logged with WARN level

Generic Exception
├── Internal Server Error (500) - Unexpected errors
└── Detected connectivity issues and logged as CRITICAL
```

#### Location
- **File**: `com.order.exception.GlobalExceptionHandler`
- **Annotations**: `@RestControllerAdvice`, `@Slf4j`

## Service Down Detection

### Product Service Down
```
REQUEST: http://localhost:8080/api/orders
BODY: {
  "productId": 1,
  "quantity": 2,
  ...
}

↓

STEP 1: Call ProductServiceClient.getProductAvailability()

↓

STEP 2: RestTemplate attempts to call http://localhost:8081/api/products/1
        (Fails with Connection refused)

↓

STEP 3: Catch ResourceAccessException

↓

STEP 4: Log ERROR:
        "Failed to connect to Product Service. Service might be down or 
        unreachable. URL: http://localhost:8081/api/products"

↓

STEP 5: Throw ProductNotAvailableException:
        "Cannot reach Product Service. Please check if the service is 
        running or try again later."

↓

RESPONSE: 400 Bad Request
{
  "success": false,
  "message": "Cannot reach Product Service. Please check if the service 
             is running or try again later.",
  "data": null,
  "timestamp": "2026-02-10T15:30:46"
}
```

### Payment Service Down
```
REQUEST: http://localhost:8080/api/orders
BODY: {
  "productId": 1,
  "quantity": 2,
  ...
}

↓

STEP 1: Product availability check succeeds ✓

↓

STEP 2: Call PaymentService.processPayment()

↓

STEP 3: External payment service enabled:
        Call PaymentServiceClient.processPaymentWithExternalService()

↓

STEP 4: RestTemplate attempts to call http://localhost:8082/api/payments
        (Fails with Connection refused)

↓

STEP 5: Catch ResourceAccessException

↓

STEP 6: Log ERROR:
        "Failed to connect to Payment Service. Service might be down or 
        unreachable. URL: http://localhost:8082/api/payments"

↓

STEP 7: Catch in PaymentService and log WARN:
        "External payment service failed. Attempting fallback to local 
        payment processing."

↓

STEP 8: Call processPaymentLocally() (Fallback method)

↓

STEP 9: Simulate local payment processing

↓

STEP 10: Log INFO:
         "Local payment processing successful. Transaction ID: ..."

↓

STEP 11: Save payment with status SUCCESS

↓

RESPONSE: 201 Created - Order created successfully
(Payment processing was done locally as fallback)
```

## Logging Levels

| Level | Meaning | Example |
|-------|---------|---------|
| DEBUG | Detailed debug information | Fetching product ID: 1 |
| INFO | General informational messages | Payment processed successfully |
| WARN | Warning messages for recoverable issues | Insufficient stock for product |
| ERROR | Error messages for problems | Product Service is unavailable |
| CRITICAL | Critical system errors | Payment Service is down |

## Configuration

### Enable/Disable External Payment Service
```properties
# Use external payment service (true/false)
payment.service.enabled=false  # Default: false (uses local processing)
```

When `payment.service.enabled=false`:
- External Payment Service is NOT called
- Payment processing is done locally
- No dependency on external payment service

When `payment.service.enabled=true`:
- Attempts to call external Payment Service
- Falls back to local processing if external service fails
- Ensures order can be created even if payment service is down

### Logging Configuration
```properties
# Set logging levels
logging.level.root=INFO
logging.level.com.order=DEBUG
```

## API Response Examples

### Product Service Down
```json
{
  "success": false,
  "message": "Cannot reach Product Service. Please check if the service is running or try again later.",
  "data": null,
  "timestamp": "2026-02-10T15:30:46"
}
```

### Payment Service Down (with fallback)
Order is created successfully with local payment processing:
```json
{
  "success": true,
  "message": "Order created successfully",
  "data": {
    "orderId": 1,
    "productId": 1,
    "quantity": 2,
    "totalPrice": 1999.98,
    "status": "CONFIRMED",
    "payment": {
      "amount": 1999.98,
      "status": "SUCCESS",
      "description": "Payment for order (Local Processing)",
      ...
    }
  },
  "timestamp": "2026-02-10T15:35:26"
}
```

## Testing Service Failures

### Simulate Product Service Down
```bash
# Stop the external Product Service if it's running
# Then try to create an order:
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 2,
    "customerName": "Test",
    "customerEmail": "test@example.com",
    "shippingAddress": "Test Address",
    "paymentMethod": "CREDIT_CARD"
  }'
```

**Check logs for:**
```
ERROR - Failed to connect to Product Service
ERROR - CRITICAL: Product Service is down or unreachable
```

### Simulate Payment Service Down
```bash
# Set payment.service.enabled=true in application.properties
# Stop the external Payment Service if it's running
# Then try to create an order:
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 2,
    ...
  }'
```

**Check logs for:**
```
ERROR - Failed to connect to Payment Service
WARN - External payment service failed. Attempting fallback to local payment processing
INFO - Processing payment locally (fallback/local mode)
INFO - Local payment processing successful
```

## Best Practices

1. **Always check logs**: Service failures are always logged with clear messages
2. **Use appropriate status codes**: Clients can identify the type of error
3. **Graceful degradation**: Payment service failures don't prevent order creation
4. **Fallback mechanisms**: Local payment processing ensures continuity
5. **Transaction safety**: Database transactions ensure data consistency
6. **Detailed logging**: Every step is logged for debugging and monitoring

## Monitoring & Alerts

### Recommended Monitoring
- Monitor log files for ERROR and CRITICAL messages
- Set up alerts for:
  - Product Service connectivity errors
  - Payment Service failures
  - Fallback payment processing activations
  - Exception rates above threshold

### Log File Location
- Configured in `application.properties` under `logging.pattern.file`
- Logs contain timestamp, thread, level, logger, and message

## Troubleshooting

### Problem: "Product Service is down"
**Solution:**
1. Check if Product Service is running on the configured URL
2. Verify network connectivity between services
3. Check Product Service logs for errors

### Problem: "Payment Service is unavailable"
**Solution:**
1. Check if Payment Service is running on the configured URL
2. Verify `payment.service.enabled` setting
3. Check payment service logs
4. If payment service is down, order will use local payment processing

### Problem: "Cannot reach services"
**Solution:**
1. Verify service URLs in `application.properties`
2. Check network connectivity
3. Ensure firewall allows communication
4. Check if services are properly registered in service discovery (if using)

## Future Enhancements

- [ ] Circuit breaker pattern for service calls
- [ ] Retry mechanism with exponential backoff
- [ ] Service health check endpoints
- [ ] Async payment processing
- [ ] Message queue integration for resilience
- [ ] Distributed tracing for debugging
- [ ] Custom metrics for monitoring

