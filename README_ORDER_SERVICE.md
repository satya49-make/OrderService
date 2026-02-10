# E-Commerce Order Microservice

A comprehensive Spring Boot microservice for managing orders, products, and payments in an e-commerce system.

## Features

✅ **Order Management**
- Create orders with product availability validation
- Retrieve orders by ID, customer email, or status
- Cancel orders
- Update order status

✅ **Payment Processing**
- Integrate with payment gateway (configurable)
- Track payment status (PENDING, SUCCESS, FAILED, REFUNDED)
- Support multiple payment methods (Credit Card, Debit Card, UPI, etc.)
- Generate unique transaction IDs

✅ **Product Management**
- Manage product inventory
- Check product availability
- Update product quantities after order confirmation
- Search products by name
- Track product status (AVAILABLE, OUT_OF_STOCK, DISCONTINUED)

✅ **Microservice Integration**
- Call external Product API for availability checks
- Extensible design for additional service integrations

✅ **Database Management**
- MySQL database with auto-schema creation
- Automatic timestamp tracking (createdAt, updatedAt)
- Transaction support for data consistency

✅ **Error Handling**
- Global exception handler
- Custom exceptions for specific scenarios
- Meaningful error responses

## Technology Stack

- **Java 17**
- **Spring Boot 4.0.1**
- **Spring Data JPA**
- **MySQL Database**
- **Lombok** (for reducing boilerplate)
- **RESTful API Design**

## Project Structure

```
src/main/java/com/order/
├── controller/
│   ├── OrderController.java
│   └── ProductController.java
├── service/
│   ├── OrderService.java
│   ├── PaymentService.java
│   └── ProductService.java
├── entity/
│   ├── Order.java
│   ├── Payment.java
│   ├── Product.java
│   ├── OrderStatus.java
│   ├── PaymentStatus.java
│   ├── PaymentMethod.java
│   └── ProductStatus.java
├── repository/
│   ├── OrderRepository.java
│   ├── PaymentRepository.java
│   └── ProductRepository.java
├── client/
│   └── ProductServiceClient.java
├── dto/
│   ├── CreateOrderRequest.java
│   ├── OrderResponse.java
│   ├── PaymentResponse.java
│   ├── ProductDTO.java
│   └── ApiResponse.java
├── exception/
│   ├── ProductNotAvailableException.java
│   ├── PaymentFailedException.java
│   ├── OrderNotFoundException.java
│   └── GlobalExceptionHandler.java
├── config/
│   └── RestTemplateConfig.java
└── OrderServiceApplication.java

resources/
└── application.properties
```

## Setup Instructions

### 1. Database Setup

Execute the SQL script to create the database and tables:

```bash
mysql -u root -p < database_setup.sql
```

Or manually run the SQL commands in your MySQL client.

### 2. Configure Application Properties

Update `src/main/resources/application.properties` with your database credentials:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/order_service_db
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 3. Build the Project

```bash
gradlew build
```

### 4. Run the Application

```bash
gradlew bootRun
```

The application will start on `http://localhost:8080/api`

## API Endpoints

### Order Management

#### Create Order
```
POST /api/orders
Content-Type: application/json

{
  "productId": 1,
  "quantity": 2,
  "customerName": "John Doe",
  "customerEmail": "john@example.com",
  "shippingAddress": "123 Main St, City, State 12345",
  "paymentMethod": "CREDIT_CARD"
}
```

**Response (201 Created):**
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
    "customerEmail": "john@example.com",
    "shippingAddress": "123 Main St, City, State 12345",
    "payment": {
      "paymentId": 1,
      "amount": 1999.98,
      "status": "SUCCESS",
      "paymentMethod": "CREDIT_CARD",
      "transactionId": "uuid-1234",
      "createdAt": "2026-02-10T10:30:00"
    },
    "createdAt": "2026-02-10T10:30:00",
    "updatedAt": "2026-02-10T10:30:00"
  },
  "timestamp": "2026-02-10T10:30:00"
}
```

#### Get Order by ID
```
GET /api/orders/{orderId}
```

#### Get Orders by Customer Email
```
GET /api/orders/customer/{email}
```

#### Get Orders by Status
```
GET /api/orders/status/{status}
```

**Status Values:** PENDING, CONFIRMED, PAYMENT_PENDING, PAYMENT_FAILED, SHIPPED, DELIVERED, CANCELLED

#### Cancel Order
```
PUT /api/orders/{orderId}/cancel
```

#### Update Order Status
```
PUT /api/orders/{orderId}/status?status={newStatus}
```

### Product Management

#### Get Product by ID
```
GET /api/products/{productId}
```

#### Get All Available Products
```
GET /api/products
```

#### Search Products by Name
```
GET /api/products/search?name={productName}
```

#### Create Product
```
POST /api/products
Content-Type: application/json

{
  "productName": "New Product",
  "description": "Product description",
  "price": 99.99,
  "availableQuantity": 100
}
```

#### Update Product
```
PUT /api/products/{productId}
Content-Type: application/json

{
  "productName": "Updated Product",
  "description": "Updated description",
  "price": 79.99,
  "availableQuantity": 150
}
```

## Order Processing Flow

1. **Accept Order Request**
   - Validate input parameters
   - Extract order details

2. **Check Product Availability**
   - Call external Product API or check local database
   - Verify sufficient stock

3. **Process Payment**
   - Create payment record
   - Call payment gateway (simulated or real)
   - Track transaction ID

4. **Confirm Order**
   - If payment succeeds:
     - Update product quantity
     - Save order with status "CONFIRMED"
     - Link payment to order
   - If payment fails:
     - Save payment failure status
     - Throw PaymentFailedException

5. **Return Response**
   - Return order details with payment information
   - Include timestamps and transaction details

## Payment Methods Supported

- CREDIT_CARD
- DEBIT_CARD
- NET_BANKING
- UPI
- WALLET
- PAYPAL

## Order Status Flow

```
PENDING
   ↓
CONFIRMED (after successful payment and product check)
   ↓
SHIPPED
   ↓
DELIVERED

Alternative paths:
- PAYMENT_PENDING (if payment processing delayed)
- PAYMENT_FAILED (if payment fails)
- CANCELLED (user cancels order)
```

## Error Handling

### Exception Types

| Exception | HTTP Status | Scenario |
|-----------|------------|----------|
| ProductNotAvailableException | 400 Bad Request | Product not found or insufficient stock |
| PaymentFailedException | 402 Payment Required | Payment processing failed |
| OrderNotFoundException | 404 Not Found | Order doesn't exist |
| General Exception | 500 Internal Server Error | Unexpected errors |

### Sample Error Response

```json
{
  "success": false,
  "message": "Insufficient stock. Required: 10, Available: 5",
  "data": null,
  "timestamp": "2026-02-10T10:30:00"
}
```

## Configuration

### Database Configuration
Edit `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/order_service_db
spring.datasource.username=root
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update
```

### API Endpoints Configuration
```properties
product.api.url=http://localhost:8081/api/products
payment.api.url=http://localhost:8082/api/payments
server.port=8080
server.servlet.context-path=/api
```

## Testing

### Sample curl commands

#### Create Order
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 2,
    "customerName": "John Doe",
    "customerEmail": "john@example.com",
    "shippingAddress": "123 Main St",
    "paymentMethod": "CREDIT_CARD"
  }'
```

#### Get Order
```bash
curl http://localhost:8080/api/orders/1
```

#### Get Products
```bash
curl http://localhost:8080/api/products
```

## Logging

The application uses SLF4J with Logback for logging. All important operations are logged:
- Order creation
- Payment processing
- Product availability checks
- Status updates
- Errors and exceptions

## Future Enhancements

- [ ] Real payment gateway integration (Stripe, PayPal)
- [ ] Order notifications (Email, SMS)
- [ ] Inventory management microservice
- [ ] User authentication and authorization
- [ ] Order tracking with tracking number
- [ ] Refund management
- [ ] Analytics and reporting
- [ ] Caching layer (Redis)
- [ ] Message queue integration (RabbitMQ, Kafka)
- [ ] API documentation (Swagger/OpenAPI)

## Troubleshooting

### Database Connection Issues
- Verify MySQL is running
- Check database credentials in `application.properties`
- Ensure database exists and is accessible

### Payment Simulation
- Current payment processing has ~95% success rate for testing
- Replace `simulatePaymentProcessing()` in PaymentService for real integration

### Product API Integration
- Ensure Product API is running on configured URL
- Check `product.api.url` in application.properties
- Handle timeouts and connection errors appropriately

## License

MIT License

## Support

For issues or questions, please create an issue in the project repository.

