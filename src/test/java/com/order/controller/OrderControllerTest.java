package com.order.controller;

import com.order.dto.CreateOrderRequest;
import com.order.dto.OrderResponse;
import com.order.entity.OrderStatus;
import com.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    private OrderController orderController;

    private CreateOrderRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderController = new OrderController(orderService);
        mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();
        request = new CreateOrderRequest(1L, 1, "Test User", "test.user@example.com", "1 Test St", "UPI");
    }

    @Test
    void createOrder_endpointShouldReturnCreated() throws Exception {
        OrderResponse resp = new OrderResponse(10L, 1L, 1, new BigDecimal("9.99"), OrderStatus.CONFIRMED.toString(), "Test User", "test.user@example.com", "1 Test St", null, LocalDateTime.now(), LocalDateTime.now());
        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(resp);

        String json = "{\"productId\":1,\"quantity\":1,\"customerName\":\"Test User\",\"customerEmail\":\"test.user@example.com\",\"shippingAddress\":\"1 Test St\",\"paymentMethod\":\"UPI\"}";

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());
    }
}
