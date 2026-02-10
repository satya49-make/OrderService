package com.order.client;

import com.order.config.ResilientWebClientHelper;
import com.order.dto.ProductDTO;
import com.order.exception.ProductNotAvailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@Slf4j
public class ProductServiceClient {

    @Value("${product.api.url}")
    private String productApiUrl;

    private final WebClient productServiceWebClient;
    private final CircuitBreaker productServiceCircuitBreaker;
    private final Retry productServiceRetry;
    private final TimeLimiter productServiceTimeLimiter;

    public ProductServiceClient(
            @Qualifier("productServiceWebClient") WebClient productServiceWebClient,
            CircuitBreaker productServiceCircuitBreaker,
            Retry productServiceRetry,
            TimeLimiter productServiceTimeLimiter) {
        this.productServiceWebClient = productServiceWebClient;
        this.productServiceCircuitBreaker = productServiceCircuitBreaker;
        this.productServiceRetry = productServiceRetry;
        this.productServiceTimeLimiter = productServiceTimeLimiter;
    }

    /**
     * Check if product is available by calling the Product API using WebClient
     * With Circuit Breaker, Retry, and TimeLimiter protection
     */
    public ProductDTO getProductAvailability(Long productId) {
        try {
            log.info("Fetching product availability for Product ID: {} (Circuit Breaker State: {})",
                    productId, productServiceCircuitBreaker.getState());

            // Execute with resilience patterns
            ProductDTO productDTO = executeWithResilience(productId);

            if (productDTO == null) {
                log.warn("Product API returned null for product ID: {}", productId);
                throw new ProductNotAvailableException("Product API returned no data for product ID: " + productId);
            }

            log.info("Successfully fetched product from Product Service - Product ID: {}, Available Quantity: {}",
                    productId, productDTO.getAvailableQuantity());
            return productDTO;

        } catch (Exception e) {
            handleProductServiceException(e, productId);
            throw e; // Exception will be handled by GlobalExceptionHandler
        }
    }

    /**
     * Execute the API call with Circuit Breaker, Retry, and TimeLimiter
     */
    private ProductDTO executeWithResilience(Long productId) {
        log.debug("Executing product API call with resilience patterns for Product ID: {}", productId);

        // Create a supplier that makes the WebClient call
        Supplier<ProductDTO> supplier = () -> {
            log.debug("Calling Product Service for Product ID: {}", productId);

            ProductDTO result = productServiceWebClient
                    .get()
                    .uri("/products/" + productId)
                    .retrieve()
                    .bodyToMono(ProductDTO.class)
                    .timeout(productServiceTimeLimiter.getTimeLimiterConfig().getTimeoutDuration())
                    .block();

            if (result != null) {
                log.debug("Product Service call succeeded for Product ID: {}", productId);
                productServiceCircuitBreaker.onSuccess(10, TimeUnit.MILLISECONDS);
            }
            return result;
        };

        // Apply retry and circuit breaker decoration
        Supplier<ProductDTO> retryDecorated = Retry.decorateSupplier(productServiceRetry, supplier);
        return productServiceCircuitBreaker.executeSupplier(retryDecorated);
    }

    /**
     * Handle different types of exceptions from Product Service
     */
    private void handleProductServiceException(Exception e, Long productId) {

        if (e instanceof WebClientResponseException) {
            WebClientResponseException wcEx = (WebClientResponseException) e;

            if (HttpStatus.SERVICE_UNAVAILABLE.value() == wcEx.getStatusCode().value()) {
                log.error("Product Service is unavailable (503). Service might be down. Product ID: {}", productId, e);
                throw new ProductNotAvailableException(
                        "Product Service is currently unavailable. Please try again later.", e);
            } else if (wcEx.getStatusCode().is5xxServerError()) {
                log.error("Product Service returned server error ({}). Error: {}",
                        wcEx.getStatusCode(), wcEx.getResponseBodyAsString(), e);
                throw new ProductNotAvailableException(
                        "Product Service encountered an error. Please try again later.", e);
            } else if (HttpStatus.NOT_FOUND.value() == wcEx.getStatusCode().value()) {
                log.warn("Product not found in Product Service - Product ID: {}. Error: {}", productId, wcEx.getMessage());
                throw new ProductNotAvailableException("Product not found: " + productId, e);
            } else if (wcEx.getStatusCode().is4xxClientError()) {
                log.error("Product Service returned client error ({}). Error: {}",
                        wcEx.getStatusCode(), wcEx.getResponseBodyAsString(), e);
                throw new ProductNotAvailableException(
                        "Invalid request to Product Service: " + e.getMessage(), e);
            }
        } else if (e instanceof java.net.ConnectException || e instanceof java.net.SocketTimeoutException) {
            log.error("Failed to connect to Product Service. Service might be down or unreachable. URL: {}, Error: {}",
                    productApiUrl, e.getMessage(), e);
            throw new ProductNotAvailableException(
                    "Cannot reach Product Service. Please check if the service is running or try again later.", e);
        } else if (e instanceof java.util.concurrent.TimeoutException) {
            log.error("Product Service request timed out. The service might be slow or unresponsive. Error: {}",
                    e.getMessage(), e);
            throw new ProductNotAvailableException(
                    "Product Service request timed out. Please try again later.", e);
        } else if (e instanceof ResilientWebClientHelper.CircuitBreakerOpenException) {
            log.error("Circuit Breaker for Product Service is OPEN. Service is temporarily unavailable.", e);
            throw new ProductNotAvailableException(
                    "Product Service is temporarily unavailable due to multiple failures. Please try again later.", e);
        } else {
            log.error("Unexpected error while fetching product availability from Product Service. Product ID: {}, Error: {}",
                    productId, e.getMessage(), e);
            throw new ProductNotAvailableException(
                    "Unexpected error occurred while checking product availability: " + e.getMessage(), e);
        }
    }
}

