package com.order.service;

import com.order.client.ProductServiceClient;
import com.order.dto.CreateOrderRequest;
import com.order.dto.OrderResponse;
import com.order.dto.PaymentResponse;
import com.order.dto.ProductDTO;
import com.order.entity.Order;
import com.order.entity.OrderStatus;
import com.order.entity.Payment;
import com.order.entity.Product;
import com.order.exception.OrderNotFoundException;
import com.order.exception.ProductNotAvailableException;
import com.order.repository.OrderRepository;
import com.order.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PaymentService paymentService;
    private final ProductServiceClient productServiceClient;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        PaymentService paymentService,
                        ProductServiceClient productServiceClient) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.paymentService = paymentService;
        this.productServiceClient = productServiceClient;
    }

    /**
     * Create and process a new order
     * Steps:
     * 1. Check product availability
     * 2. Process payment
     * 3. Update product quantity
     * 4. Save order with payment details
     */
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for product ID: {} with quantity: {}", request.getProductId(), request.getQuantity());

        // Step 1: Check product availability
        ProductDTO productDTO = checkProductAvailability(request.getProductId(), request.getQuantity());

        // Step 2: Calculate total price
        BigDecimal totalPrice = productDTO.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        // Step 3: Process payment
        Payment payment = paymentService.processPayment(totalPrice, request.getPaymentMethod());

        // Step 4: If payment is successful, proceed with order creation
        if (payment.getStatus().toString().equals("SUCCESS")) {
            Order order = new Order();
            order.setProductId(request.getProductId());
            order.setQuantity(request.getQuantity());
            order.setTotalPrice(totalPrice);
            order.setCustomerName(request.getCustomerName());
            order.setCustomerEmail(request.getCustomerEmail());
            order.setShippingAddress(request.getShippingAddress());
            order.setStatus(OrderStatus.CONFIRMED);
            order.setPayment(payment);

            // Step 5: Update product quantity in database
            updateProductQuantity(request.getProductId(), request.getQuantity());

            // Step 6: Save order
            Order savedOrder = orderRepository.save(order);
            log.info("Order created successfully with ID: {}", savedOrder.getOrderId());

            return convertToResponse(savedOrder);
        } else {
            throw new RuntimeException("Payment failed. Order cannot be created.");
        }
    }

    /**
     * Check product availability by calling Product API
     */
    private ProductDTO checkProductAvailability(Long productId, Integer requiredQuantity) {
        try {
            log.info("Checking product availability for Product ID: {} with required quantity: {}", productId, requiredQuantity);

            ProductDTO productDTO = productServiceClient.getProductAvailability(productId);

            if (productDTO == null) {
                log.warn("Product not found or Product Service returned null for Product ID: {}", productId);
                throw new ProductNotAvailableException("Product not found: " + productId);
            }

            if (productDTO.getAvailableQuantity() < requiredQuantity) {
                log.warn("Insufficient stock for Product ID: {}. Required: {}, Available: {}",
                        productId, requiredQuantity, productDTO.getAvailableQuantity());
                throw new ProductNotAvailableException(
                        "Insufficient stock. Required: " + requiredQuantity +
                        ", Available: " + productDTO.getAvailableQuantity());
            }

            log.info("Product {} is available with sufficient quantity: {}", productId, productDTO.getAvailableQuantity());
            return productDTO;

        } catch (ProductNotAvailableException e) {
            log.error("Product availability check failed for Product ID: {}. Error: {}", productId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while checking product availability for Product ID: {}. Error: {}",
                    productId, e.getMessage(), e);
            throw new ProductNotAvailableException(
                    "Failed to check product availability: " + e.getMessage(), e);
        }
    }

    /**
     * Update product quantity after successful order
     */
    private void updateProductQuantity(Long productId, Integer orderedQuantity) {
        try {
            log.info("Updating product quantity for Product ID: {}, Ordered Quantity: {}", productId, orderedQuantity);

            Product product = productRepository.findByProductId(productId)
                    .orElseThrow(() -> {
                        log.error("Product not found in database for Product ID: {}", productId);
                        return new ProductNotAvailableException("Product not found: " + productId);
                    });

            int newQuantity = product.getAvailableQuantity() - orderedQuantity;
            product.setAvailableQuantity(newQuantity);

            productRepository.save(product);
            log.info("Product quantity updated successfully. Product ID: {}, Previous Quantity: {}, New Quantity: {}",
                    productId, product.getAvailableQuantity() + orderedQuantity, newQuantity);

        } catch (ProductNotAvailableException e) {
            log.error("Failed to update product quantity. Product ID: {}, Error: {}", productId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while updating product quantity for Product ID: {}. Error: {}",
                    productId, e.getMessage(), e);
            throw new RuntimeException("Failed to update product quantity: " + e.getMessage(), e);
        }
    }

    /**
     * Get order by ID
     */
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
        return convertToResponse(order);
    }

    /**
     * Get all orders by customer email
     */
    public List<OrderResponse> getOrdersByCustomerEmail(String email) {
        List<Order> orders = orderRepository.findByCustomerEmail(email);
        return orders.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all orders with specific status
     */
    public List<OrderResponse> getOrdersByStatus(String status) {
        OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
        List<Order> orders = orderRepository.findByStatus(orderStatus);
        return orders.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Cancel an order
     */
    public OrderResponse cancelOrder(Long orderId) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Order is already cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updatedOrder = orderRepository.save(order);

        log.info("Order {} cancelled successfully", orderId);
        return convertToResponse(updatedOrder);
    }

    /**
     * Update order status
     */
    public OrderResponse updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        OrderStatus status = OrderStatus.valueOf(newStatus.toUpperCase());
        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);

        log.info("Order {} status updated to {}", orderId, status);
        return convertToResponse(updatedOrder);
    }

    /**
     * Convert Order entity to OrderResponse DTO
     */
    private OrderResponse convertToResponse(Order order) {
        PaymentResponse paymentResponse = null;
        if (order.getPayment() != null) {
            paymentResponse = paymentService.convertToResponse(order.getPayment());
        }

        return new OrderResponse(
                order.getOrderId(),
                order.getProductId(),
                order.getQuantity(),
                order.getTotalPrice(),
                order.getStatus().toString(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getShippingAddress(),
                paymentResponse,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}

