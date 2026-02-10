package com.order.service;

import com.order.client.ProductServiceClient;
import com.order.dto.CreateOrderRequest;
import com.order.dto.OrderResponse;
import com.order.dto.ProductDTO;
import com.order.entity.Order;
import com.order.entity.Payment;
import com.order.entity.Product;
import com.order.entity.ProductStatus;
import com.order.repository.OrderRepository;
import com.order.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private ProductServiceClient productServiceClient;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest request;

    @BeforeEach
    void setUp() {
        request = new CreateOrderRequest(1L, 2, "John Test", "john.test@example.com", "123 Test St", "CREDIT_CARD");
    }

    @Test
    void createOrder_successfulFlow_shouldReturnOrderResponse() {
        // Arrange
        ProductDTO productDTO = new ProductDTO(1L, "Test Product", "Desc",
                new BigDecimal("10.00"), 5, "AVAILABLE");

        when(productServiceClient.getProductAvailability(1L)).thenReturn(productDTO);

        BigDecimal expectedTotal = productDTO.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        Payment payment = new Payment();
        payment.setPaymentId(11L);
        payment.setAmount(expectedTotal);
        payment.setStatus(com.order.entity.PaymentStatus.SUCCESS);
        payment.setPaymentMethod(com.order.entity.PaymentMethod.CREDIT_CARD);
        payment.setTransactionId("tx-123");
        payment.setCreatedAt(LocalDateTime.now());
        when(paymentService.processPayment(expectedTotal, request.getPaymentMethod())).thenReturn(payment);

        Product product = new Product(1L, "Test Product", "Desc", productDTO.getPrice(), 5, ProductStatus.AVAILABLE, LocalDateTime.now(), LocalDateTime.now());
        when(productRepository.findByProductId(1L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setOrderId(101L);
            return o;
        });

        // Act
        OrderResponse response = orderService.createOrder(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(101L);
        assertThat(response.getTotalPrice()).isEqualByComparingTo(expectedTotal);

        // Verify product quantity update saved
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, times(1)).save(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();
        assertThat(savedProduct.getAvailableQuantity()).isEqualTo(3); // 5 - 2

        verify(orderRepository, times(1)).save(any(Order.class));
    }
}

