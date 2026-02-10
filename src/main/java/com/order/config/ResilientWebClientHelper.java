package com.order.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.function.Supplier;

@Component
@Slf4j
public class ResilientWebClientHelper {

    /**
     * Execute a blocking call with Circuit Breaker and Retry protection
     * Used for synchronous calls with full resilience decoration
     */
    public <T> T executeWithResilienceBlocking(
            Supplier<T> supplier,
            CircuitBreaker circuitBreaker,
            Retry retry,
            String serviceName) {

        try {
            log.debug("Executing resilient blocking call for: {}", serviceName);

            // Apply retry decoration
            Supplier<T> retryDecorated = Retry.decorateSupplier(retry, supplier);

            // Apply circuit breaker decoration
            return circuitBreaker.executeSupplier(retryDecorated);

        } catch (Exception e) {
            log.error("Resilient blocking call failed for {}: {}", serviceName, e.getMessage(), e);
            throw e;
        }
    }


    /**
     * Determine if a request should be retried based on the exception
     */
    public boolean shouldRetry(Throwable throwable, String serviceName) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException ex = (WebClientResponseException) throwable;

            // Retry on 5xx errors (server errors)
            if (ex.getStatusCode().is5xxServerError()) {
                log.warn("Retrying {} due to server error: {}", serviceName, ex.getStatusCode());
                return true;
            }

            // Don't retry on 4xx errors (client errors)
            if (ex.getStatusCode().is4xxClientError()) {
                log.error("Not retrying {} due to client error: {}", serviceName, ex.getStatusCode());
                return false;
            }
        }

        // Retry on timeout and connection errors
        if (throwable instanceof java.util.concurrent.TimeoutException ||
                throwable instanceof java.net.ConnectException ||
                throwable instanceof java.net.SocketTimeoutException) {
            log.warn("Retrying {} due to timeout/connection error: {}", serviceName, throwable.getMessage());
            return true;
        }

        return false;
    }

    /**
     * Custom exception for circuit breaker open state
     */
    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message, Throwable cause) {
            super(message, cause);
        }

        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
}

