package com.order.controller;

import com.order.dto.ApiResponse;
import com.order.dto.CreateOrderRequest;
import com.order.dto.OrderResponse;
import com.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@Slf4j
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Create a new order with product availability check and payment processing
     * POST /api/orders
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@RequestBody CreateOrderRequest request) {
        log.info("Creating new order for product ID: {}", request.getProductId());
        OrderResponse orderResponse = orderService.createOrder(request);
        ApiResponse<OrderResponse> response = new ApiResponse<>(true, "Order created successfully", orderResponse);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get order by ID
     * GET /api/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long orderId) {
        log.info("Fetching order with ID: {}", orderId);
        OrderResponse orderResponse = orderService.getOrderById(orderId);
        ApiResponse<OrderResponse> response = new ApiResponse<>(true, "Order retrieved successfully", orderResponse);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Get all orders by customer email
     * GET /api/orders/customer/{email}
     */
    @GetMapping("/customer/{email}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByCustomerEmail(@PathVariable String email) {
        log.info("Fetching orders for customer email: {}", email);
        List<OrderResponse> orders = orderService.getOrdersByCustomerEmail(email);
        ApiResponse<List<OrderResponse>> response = new ApiResponse<>(true, "Orders retrieved successfully", orders);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Get all orders with specific status
     * GET /api/orders/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByStatus(@PathVariable String status) {
        log.info("Fetching orders with status: {}", status);
        List<OrderResponse> orders = orderService.getOrdersByStatus(status);
        ApiResponse<List<OrderResponse>> response = new ApiResponse<>(true, "Orders retrieved successfully", orders);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Cancel an order
     * PUT /api/orders/{orderId}/cancel
     */
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable Long orderId) {
        log.info("Cancelling order with ID: {}", orderId);
        OrderResponse orderResponse = orderService.cancelOrder(orderId);
        ApiResponse<OrderResponse> response = new ApiResponse<>(true, "Order cancelled successfully", orderResponse);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Update order status
     * PUT /api/orders/{orderId}/status
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status) {
        log.info("Updating order {} status to: {}", orderId, status);
        OrderResponse orderResponse = orderService.updateOrderStatus(orderId, status);
        ApiResponse<OrderResponse> response = new ApiResponse<>(true, "Order status updated successfully", orderResponse);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
