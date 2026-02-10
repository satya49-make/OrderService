package com.order.service;

import com.order.entity.Payment;
import com.order.entity.PaymentMethod;
import com.order.entity.PaymentStatus;
import com.order.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        // nothing special required here
    }

    @Test
    void convertToResponse_shouldReturnPaymentResponse() {
        Payment payment = new Payment();
        payment.setPaymentId(55L);
        payment.setAmount(new BigDecimal("45.00"));
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentMethod(PaymentMethod.UPI);
        payment.setTransactionId("tx-abc");
        payment.setCreatedAt(LocalDateTime.now());

        var resp = paymentService.convertToResponse(payment);

        assertThat(resp).isNotNull();
        assertThat(resp.getPaymentId()).isEqualTo(55L);
        assertThat(resp.getAmount()).isEqualByComparingTo(new BigDecimal("45.00"));
        assertThat(resp.getStatus()).isEqualTo("SUCCESS");
        assertThat(resp.getPaymentMethod()).isEqualTo("UPI");
        assertThat(resp.getTransactionId()).isEqualTo("tx-abc");
    }

    @Test
    void getPaymentById_shouldReturnPayment_whenExists() {
        Payment payment = new Payment();
        payment.setPaymentId(99L);
        payment.setAmount(new BigDecimal("100.00"));
        payment.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId("tx-xyz");
        payment.setCreatedAt(LocalDateTime.now());

        when(paymentRepository.findById(99L)).thenReturn(Optional.of(payment));

        var got = paymentService.getPaymentById(99L);
        assertThat(got).isNotNull();
        assertThat(got.getPaymentId()).isEqualTo(99L);
    }
}
