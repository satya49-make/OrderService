# E-Commerce Order Service - API Test Examples

## Base URL
```
http://localhost:8080/api
```

## 1. Product Management Tests

### 1.1 Create Products
```bash
# Create Product 1
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "Laptop",
    "description": "High-performance laptop",
    "price": 999.99,
    "availableQuantity": 50
  }'

# Create Product 2
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "Wireless Mouse",
    "description": "Ergonomic wireless mouse",
    "price": 29.99,
    "availableQuantity": 200
  }'

# Create Product 3
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "USB-C Cable",
    "description": "High-speed charging cable",
    "price": 15.99,
    "availableQuantity": 500
  }'
```

### 1.2 Get All Available Products
```bash
curl http://localhost:8080/api/products
```

### 1.3 Get Specific Product
```bash
curl http://localhost:8080/api/products/1
```

### 1.4 Search Products
```bash
curl "http://localhost:8080/api/products/search?name=Laptop"
```

### 1.5 Update Product
```bash
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "Gaming Laptop",
    "description": "High-performance gaming laptop",
    "price": 1299.99,
    "availableQuantity": 30
  }'
```

## 2. Order Management Tests

### 2.1 Create Order - Success Case
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 2,
    "customerName": "John Doe",
    "customerEmail": "john.doe@example.com",
    "shippingAddress": "123 Main Street, New York, NY 10001",
    "paymentMethod": "CREDIT_CARD"
  }'
```

**Expected Response (201 Created):**
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
    "customerName": "John Doe",
    "customerEmail": "john.doe@example.com",
    "shippingAddress": "123 Main Street, New York, NY 10001",
    "payment": {
      "paymentId": 1,
      "amount": 1999.98,
      "status": "SUCCESS",
      "paymentMethod": "CREDIT_CARD",
      "transactionId": "550e8400-e29b-41d4-a716-446655440000",
      "createdAt": "2026-02-10T10:30:45"
    },
    "createdAt": "2026-02-10T10:30:45",
    "updatedAt": "2026-02-10T10:30:45"
  },
  "timestamp": "2026-02-10T10:30:45"
}
```

### 2.2 Create Order - Insufficient Stock
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 1000,
    "customerName": "Jane Smith",
    "customerEmail": "jane.smith@example.com",
    "shippingAddress": "456 Oak Avenue, Los Angeles, CA 90001",
    "paymentMethod": "DEBIT_CARD"
  }'
```

**Expected Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Insufficient stock. Required: 1000, Available: 48",
  "data": null,
  "timestamp": "2026-02-10T10:31:00"
}
```

### 2.3 Create Multiple Orders for Same Customer
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 2,
    "quantity": 5,
    "customerName": "John Doe",
    "customerEmail": "john.doe@example.com",
    "shippingAddress": "123 Main Street, New York, NY 10001",
    "paymentMethod": "UPI"
  }'
```

### 2.4 Get Order by ID
```bash
curl http://localhost:8080/api/orders/1
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Order retrieved successfully",
  "data": {
    "orderId": 1,
    "productId": 1,
    "quantity": 2,
    "totalPrice": 1999.98,
    "status": "CONFIRMED",
    "customerName": "John Doe",
    "customerEmail": "john.doe@example.com",
    "shippingAddress": "123 Main Street, New York, NY 10001",
    "payment": {
      "paymentId": 1,
      "amount": 1999.98,
      "status": "SUCCESS",
      "paymentMethod": "CREDIT_CARD",
      "transactionId": "550e8400-e29b-41d4-a716-446655440000",
      "createdAt": "2026-02-10T10:30:45"
    },
    "createdAt": "2026-02-10T10:30:45",
    "updatedAt": "2026-02-10T10:30:45"
  },
  "timestamp": "2026-02-10T10:31:10"
}
```

### 2.5 Get Orders by Customer Email
```bash
curl "http://localhost:8080/api/orders/customer/john.doe@example.com"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Orders retrieved successfully",
  "data": [
    {
      "orderId": 1,
      "productId": 1,
      "quantity": 2,
      "totalPrice": 1999.98,
      "status": "CONFIRMED",
      "customerName": "John Doe",
      "customerEmail": "john.doe@example.com",
      "shippingAddress": "123 Main Street, New York, NY 10001",
      "payment": { ... },
      "createdAt": "2026-02-10T10:30:45",
      "updatedAt": "2026-02-10T10:30:45"
    },
    {
      "orderId": 2,
      "productId": 2,
      "quantity": 5,
      "totalPrice": 149.95,
      "status": "CONFIRMED",
      "customerName": "John Doe",
      "customerEmail": "john.doe@example.com",
      "shippingAddress": "123 Main Street, New York, NY 10001",
      "payment": { ... },
      "createdAt": "2026-02-10T10:31:05",
      "updatedAt": "2026-02-10T10:31:05"
    }
  ],
  "timestamp": "2026-02-10T10:31:20"
}
```

### 2.6 Get Orders by Status
```bash
curl "http://localhost:8080/api/orders/status/CONFIRMED"
```

### 2.7 Update Order Status
```bash
curl -X PUT "http://localhost:8080/api/orders/1/status?status=SHIPPED" \
  -H "Content-Type: application/json"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Order status updated successfully",
  "data": {
    "orderId": 1,
    "productId": 1,
    "quantity": 2,
    "totalPrice": 1999.98,
    "status": "SHIPPED",
    "customerName": "John Doe",
    "customerEmail": "john.doe@example.com",
    "shippingAddress": "123 Main Street, New York, NY 10001",
    "payment": { ... },
    "createdAt": "2026-02-10T10:30:45",
    "updatedAt": "2026-02-10T10:31:25"
  },
  "timestamp": "2026-02-10T10:31:25"
}
```

### 2.8 Cancel Order
```bash
curl -X PUT http://localhost:8080/api/orders/1/cancel \
  -H "Content-Type: application/json"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Order cancelled successfully",
  "data": {
    "orderId": 1,
    "productId": 1,
    "quantity": 2,
    "totalPrice": 1999.98,
    "status": "CANCELLED",
    "customerName": "John Doe",
    "customerEmail": "john.doe@example.com",
    "shippingAddress": "123 Main Street, New York, NY 10001",
    "payment": { ... },
    "createdAt": "2026-02-10T10:30:45",
    "updatedAt": "2026-02-10T10:31:30"
  },
  "timestamp": "2026-02-10T10:31:30"
}
```

## 3. Payment Method Examples

### Supported Payment Methods
```
- CREDIT_CARD
- DEBIT_CARD
- NET_BANKING
- UPI
- WALLET
- PAYPAL
```

### Examples with Different Payment Methods

```bash
# Credit Card
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 1,
    "customerName": "Alice Johnson",
    "customerEmail": "alice@example.com",
    "shippingAddress": "789 Pine Road, Chicago, IL 60601",
    "paymentMethod": "CREDIT_CARD"
  }'

# UPI
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 2,
    "quantity": 3,
    "customerName": "Bob Wilson",
    "customerEmail": "bob@example.com",
    "shippingAddress": "321 Elm Street, Houston, TX 77001",
    "paymentMethod": "UPI"
  }'

# PayPal
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 3,
    "quantity": 10,
    "customerName": "Carol Davis",
    "customerEmail": "carol@example.com",
    "shippingAddress": "654 Maple Lane, Phoenix, AZ 85001",
    "paymentMethod": "PAYPAL"
  }'
```

## 4. Database Verification

### Check Orders
```sql
SELECT * FROM orders;
SELECT * FROM payments;
SELECT * FROM products;
```

### Check Product Inventory After Orders
```sql
SELECT product_id, product_name, available_quantity FROM products;
-- Expected: Quantities should be reduced based on ordered amounts
```

## 5. Error Scenarios

### Order Not Found
```bash
curl http://localhost:8080/api/orders/999
```

**Expected Response (404 Not Found):**
```json
{
  "success": false,
  "message": "Order not found with ID: 999",
  "data": null,
  "timestamp": "2026-02-10T10:32:00"
}
```

### Invalid Payment Method
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 1,
    "customerName": "Test User",
    "customerEmail": "test@example.com",
    "shippingAddress": "Test Address",
    "paymentMethod": "INVALID_METHOD"
  }'
```

**Expected Response (500 Internal Server Error):**
```json
{
  "success": false,
  "message": "Payment processing error: Invalid payment method: INVALID_METHOD",
  "data": null,
  "timestamp": "2026-02-10T10:32:15"
}
```

## Testing Steps

1. **Start the application:**
   ```bash
   gradlew bootRun
   ```

2. **Setup database (first time only):**
   ```bash
   mysql -u root -p < database_setup.sql
   ```

3. **Create sample products** (Section 1.1)

4. **Create orders** (Section 2.1)

5. **Verify in database:**
   ```bash
   mysql -u root -p -e "SELECT * FROM order_service_db.orders;"
   ```

6. **Test all endpoints** as documented above

## Performance Testing

To test with multiple concurrent orders:
```bash
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d "{
      \"productId\": 2,
      \"quantity\": 1,
      \"customerName\": \"Customer $i\",
      \"customerEmail\": \"customer$i@example.com\",
      \"shippingAddress\": \"Address $i\",
      \"paymentMethod\": \"CREDIT_CARD\"
    }" &
done
wait
```

## Notes

- The payment processing is simulated with a 95% success rate
- Replace the `simulatePaymentProcessing()` method for real payment gateway integration
- Product API integration can be configured in `application.properties`
- All timestamps are in UTC timezone
- Transactions are atomic - if payment fails, the order won't be created

