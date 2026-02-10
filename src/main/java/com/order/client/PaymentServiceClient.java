package com.order.client;

import com.order.dto.PaymentRequestDTO;
import com.order.dto.PaymentResponseDTO;
import com.order.exception.PaymentFailedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@Slf4j
public class PaymentServiceClient {

    @Value("${payment.api.url}")
    private String paymentApiUrl;

    private final WebClient paymentServiceWebClient;
    private final CircuitBreaker paymentServiceCircuitBreaker;
    private final Retry paymentServiceRetry;
    private final TimeLimiter paymentServiceTimeLimiter;

    public PaymentServiceClient(
            @Qualifier("paymentServiceWebClient") WebClient paymentServiceWebClient,
            CircuitBreaker paymentServiceCircuitBreaker,
            Retry paymentServiceRetry,
            TimeLimiter paymentServiceTimeLimiter) {
        this.paymentServiceWebClient = paymentServiceWebClient;
        this.paymentServiceCircuitBreaker = paymentServiceCircuitBreaker;
        this.paymentServiceRetry = paymentServiceRetry;
        this.paymentServiceTimeLimiter = paymentServiceTimeLimiter;
    }

    /**
     * Call external payment service to process payment
     * With Circuit Breaker, Retry, and TimeLimiter protection
     */
    public PaymentResponseDTO processPaymentWithExternalService(PaymentRequestDTO paymentRequest) {
        try {
            log.info("Processing payment with external Payment Service. Amount: {}, Method: {}, Circuit Breaker State: {}",
                    paymentRequest.getAmount(), paymentRequest.getPaymentMethod(),
                    paymentServiceCircuitBreaker.getState());

            PaymentResponseDTO response = executeWithResilience(paymentRequest);

            if (response == null) {
                log.warn("Payment Service returned null response for amount: {}", paymentRequest.getAmount());
                throw new PaymentFailedException("Payment Service returned no response");
            }

            log.info("Payment processed successfully with external service. Transaction ID: {}, Status: {}",
                    response.getTransactionId(), response.getStatus());
            return response;

        } catch (Exception e) {
            handlePaymentServiceException(e, paymentRequest);
            throw e; // Exception will be handled by GlobalExceptionHandler
        }
    }

    /**
     * Execute the payment API call with Circuit Breaker, Retry, and TimeLimiter
     * Uses manual decoration with resilience patterns
     */
    private PaymentResponseDTO executeWithResilience(PaymentRequestDTO paymentRequest) {
        log.debug("Executing payment API call with resilience patterns for amount: {}", paymentRequest.getAmount());

        // Create a supplier that makes the WebClient call
        Supplier<PaymentResponseDTO> supplier = () -> {
            log.debug("Calling Payment Service for amount: {}", paymentRequest.getAmount());

            PaymentResponseDTO result = paymentServiceWebClient
                    .post()
                    .uri("/payments/process")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(paymentRequest)
                    .retrieve()
                    .bodyToMono(PaymentResponseDTO.class)
                    .timeout(paymentServiceTimeLimiter.getTimeLimiterConfig().getTimeoutDuration())
                    .block();

            if (result != null) {
                log.debug("Payment Service call succeeded for amount: {}", paymentRequest.getAmount());
                paymentServiceCircuitBreaker.onSuccess(100, TimeUnit.MICROSECONDS);
            }
            return result;
        };

        // Apply retry and circuit breaker decoration
        Supplier<PaymentResponseDTO> retryDecorated = Retry.decorateSupplier(paymentServiceRetry, supplier);
        return paymentServiceCircuitBreaker.executeSupplier(retryDecorated);
    }

    /**
     * Handle different types of exceptions from Payment Service
     */
    private void handlePaymentServiceException(Exception e, PaymentRequestDTO paymentRequest) {

        if (e instanceof WebClientResponseException) {
            WebClientResponseException wcEx = (WebClientResponseException) e;

            if (HttpStatus.SERVICE_UNAVAILABLE.value() == wcEx.getStatusCode().value()) {
                log.error("Payment Service is unavailable (503). Service might be down. Amount: {}",
                        paymentRequest.getAmount(), e);
                throw new PaymentFailedException(
                        "Payment Service is currently unavailable. Please try again later.", e);
            } else if (HttpStatus.GATEWAY_TIMEOUT.value() == wcEx.getStatusCode().value()) {
                log.error("Payment Service gateway timeout. Request took too long to process. Amount: {}",
                        paymentRequest.getAmount(), e);
                throw new PaymentFailedException(
                        "Payment Service request timed out. Please try again later.", e);
            } else if (wcEx.getStatusCode().is5xxServerError()) {
                log.error("Payment Service returned server error ({}). Error: {}",
                        wcEx.getStatusCode(), wcEx.getResponseBodyAsString(), e);
                throw new PaymentFailedException(
                        "Payment Service encountered an error. Please try again later.", e);
            } else if (HttpStatus.BAD_REQUEST.value() == wcEx.getStatusCode().value()) {
                log.error("Invalid payment request to Payment Service. Error: {}", wcEx.getResponseBodyAsString(), e);
                throw new PaymentFailedException(
                        "Invalid payment details provided: " + e.getMessage(), e);
            } else if (wcEx.getStatusCode().is4xxClientError()) {
                log.error("Payment Service returned client error ({}). Error: {}",
                        wcEx.getStatusCode(), wcEx.getResponseBodyAsString(), e);
                throw new PaymentFailedException(
                        "Payment Service request failed: " + e.getMessage(), e);
            }
        } else if (e instanceof java.net.ConnectException || e instanceof java.net.SocketTimeoutException) {
            log.error("Failed to connect to Payment Service. Service might be down or unreachable. URL: {}, Error: {}",
                    paymentApiUrl, e.getMessage(), e);
            throw new PaymentFailedException(
                    "Cannot reach Payment Service. The service might be offline. Please try again later.", e);
        } else if (e instanceof java.util.concurrent.TimeoutException) {
            log.error("Payment Service request timed out. The service might be slow or unresponsive. Error: {}",
                    e.getMessage(), e);
            throw new PaymentFailedException(
                    "Payment Service request timed out. Please try again later.", e);
        } else {
            log.error("Unexpected error while processing payment with external Payment Service. Error: {}",
                    e.getMessage(), e);
            throw new PaymentFailedException(
                    "Unexpected error occurred during payment processing: " + e.getMessage(), e);
        }
    }
}

