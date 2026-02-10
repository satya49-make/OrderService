package com.order.service;

import com.order.client.PaymentServiceClient;
import com.order.dto.PaymentRequestDTO;
import com.order.dto.PaymentResponse;
import com.order.dto.PaymentResponseDTO;
import com.order.entity.Order;
import com.order.entity.Payment;
import com.order.entity.PaymentMethod;
import com.order.entity.PaymentStatus;
import com.order.exception.PaymentFailedException;
import com.order.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentServiceClient paymentServiceClient;

    @Value("${payment.service.enabled:false}")
    private boolean externalPaymentServiceEnabled;

    public PaymentService(PaymentRepository paymentRepository, PaymentServiceClient paymentServiceClient) {
        this.paymentRepository = paymentRepository;
        this.paymentServiceClient = paymentServiceClient;
    }

    /**
     * Process payment for an order
     * Attempts to use external payment service if enabled, falls back to local processing on failure
     */
    public Payment processPayment(BigDecimal amount, String paymentMethodStr) {
        try {
            log.info("Processing payment for amount: {}, Method: {}", amount, paymentMethodStr);

            PaymentMethod paymentMethod = PaymentMethod.valueOf(paymentMethodStr.toUpperCase());
            String transactionId = UUID.randomUUID().toString();

            // Try external payment service first if enabled
            if (externalPaymentServiceEnabled) {
                return processPaymentWithExternalService(amount, paymentMethod, transactionId);
            } else {
                // Fall back to local payment processing
                log.info("External payment service is disabled. Using local payment processing.");
                return processPaymentLocally(amount, paymentMethod, transactionId);
            }

        } catch (IllegalArgumentException e) {
            log.error("Invalid payment method provided: {}", paymentMethodStr, e);
            throw new PaymentFailedException("Invalid payment method: " + paymentMethodStr);
        } catch (PaymentFailedException e) {
            log.error("Payment failed: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during payment processing: {}", e.getMessage(), e);
            throw new PaymentFailedException("Payment processing error: " + e.getMessage(), e);
        }
    }

    /**
     * Process payment using external payment service
     * Falls back to local processing if external service is down
     */
    private Payment processPaymentWithExternalService(BigDecimal amount, PaymentMethod paymentMethod, String transactionId) {
        try {
            log.info("Attempting to process payment with external Payment Service. Transaction ID: {}", transactionId);

            PaymentRequestDTO paymentRequest = new PaymentRequestDTO(
                    amount,
                    paymentMethod.toString(),
                    transactionId,
                    "Payment for order"
            );

            PaymentResponseDTO paymentResponse = paymentServiceClient.processPaymentWithExternalService(paymentRequest);

            if (paymentResponse != null && paymentResponse.getStatus() != null) {
                log.info("External payment service response received. Status: {}, Transaction ID: {}",
                        paymentResponse.getStatus(), paymentResponse.getTransactionId());

                Payment payment = new Payment();
                payment.setAmount(amount);
                payment.setPaymentMethod(paymentMethod);
                payment.setTransactionId(transactionId);
                payment.setStatus(PaymentStatus.valueOf(paymentResponse.getStatus().toUpperCase()));
                payment.setDescription(paymentResponse.getDescription());

                Payment savedPayment = paymentRepository.save(payment);
                log.info("Payment saved successfully. Payment ID: {}, Status: {}", savedPayment.getPaymentId(), savedPayment.getStatus());
                return savedPayment;
            } else {
                log.error("External payment service returned invalid response");
                throw new PaymentFailedException("Invalid response from Payment Service");
            }

        } catch (PaymentFailedException e) {
            log.warn("External payment service failed. Attempting fallback to local payment processing. Error: {}", e.getMessage());
            return processPaymentLocally(amount, paymentMethod, transactionId);
        } catch (Exception e) {
            log.error("Unexpected error with external payment service: {}. Falling back to local processing.", e.getMessage(), e);
            return processPaymentLocally(amount, paymentMethod, transactionId);
        }
    }

    /**
     * Process payment locally (fallback method when external service is unavailable)
     */
    private Payment processPaymentLocally(BigDecimal amount, PaymentMethod paymentMethod, String transactionId) {
        try {
            log.info("Processing payment locally (fallback/local mode). Transaction ID: {}", transactionId);

            Payment payment = new Payment();
            payment.setAmount(amount);
            payment.setPaymentMethod(paymentMethod);
            payment.setTransactionId(transactionId);
            payment.setDescription("Payment for order (Local Processing)");

            // Simulate local payment processing
            if (simulatePaymentProcessing()) {
                payment.setStatus(PaymentStatus.SUCCESS);
                log.info("Local payment processing successful. Transaction ID: {}", transactionId);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                log.error("Local payment processing failed. Transaction ID: {}", transactionId);
                Payment savedPayment = paymentRepository.save(payment);
                throw new PaymentFailedException("Local payment processing failed. Please try again.");
            }

            Payment savedPayment = paymentRepository.save(payment);
            log.info("Local payment saved successfully. Payment ID: {}, Status: {}", savedPayment.getPaymentId(), savedPayment.getStatus());
            return savedPayment;

        } catch (PaymentFailedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error in local payment processing: {}", e.getMessage(), e);
            throw new PaymentFailedException("Local payment processing error: " + e.getMessage(), e);
        }
    }

    /**
     * Simulate payment processing - replace with actual payment gateway integration
     */
    private boolean simulatePaymentProcessing() {
        // Simulate 95% success rate
        return Math.random() < 0.95;
    }

    /**
     * Convert Payment entity to PaymentResponse DTO
     */
    public PaymentResponse convertToResponse(Payment payment) {
        if (payment == null) {
            return null;
        }
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getAmount(),
                payment.getStatus().toString(),
                payment.getPaymentMethod().toString(),
                payment.getTransactionId(),
                payment.getCreatedAt()
        );
    }

    /**
     * Get payment by ID
     */
    public Payment getPaymentById(Long paymentId) {
        try {
            log.info("Fetching payment with ID: {}", paymentId);
            return paymentRepository.findById(paymentId)
                    .orElseThrow(() -> {
                        log.warn("Payment not found with ID: {}", paymentId);
                        return new RuntimeException("Payment not found with ID: " + paymentId);
                    });
        } catch (Exception e) {
            log.error("Error fetching payment with ID: {}", paymentId, e);
            throw e;
        }
    }
}

