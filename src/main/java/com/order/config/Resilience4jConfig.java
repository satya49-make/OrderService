package com.order.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Configuration
@Slf4j
public class Resilience4jConfig {

    // ==================== Circuit Breaker Configuration ====================

    /**
     * Configure CircuitBreaker for Product Service
     * - Failure rate threshold: 50%
     * - Sliding window size: 10
     * - Minimum number of calls to calculate failure rate: 5
     * - Automatic transition to HALF_OPEN after 30 seconds
     */
    @Bean
    public CircuitBreakerConfig productServiceCircuitBreakerConfig() {
        log.info("Configuring Circuit Breaker for Product Service");
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slowCallRateThreshold(50)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .recordException(e -> e instanceof TimeoutException
                        || e instanceof java.net.ConnectException
                        || e instanceof java.net.SocketTimeoutException)
                .build();
    }

    /**
     * Configure CircuitBreaker for Payment Service
     * - More lenient than Product Service (70% failure threshold)
     * - Shorter wait duration (15 seconds)
     */
    @Bean
    public CircuitBreakerConfig paymentServiceCircuitBreakerConfig() {
        log.info("Configuring Circuit Breaker for Payment Service");
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(70)
                .slowCallRateThreshold(50)
                .slowCallDurationThreshold(Duration.ofSeconds(3))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .waitDurationInOpenState(Duration.ofSeconds(15))
                .recordException(e -> e instanceof TimeoutException
                        || e instanceof java.net.ConnectException
                        || e instanceof java.net.SocketTimeoutException)
                .build();
    }

    /**
     * Create CircuitBreaker for Product Service
     */
    @Bean
    public CircuitBreaker productServiceCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(
                "productServiceCircuitBreaker",
                productServiceCircuitBreakerConfig()
        );
        logCircuitBreakerEvents(circuitBreaker);
        return circuitBreaker;
    }

    /**
     * Create CircuitBreaker for Payment Service
     */
    @Bean
    public CircuitBreaker paymentServiceCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(
                "paymentServiceCircuitBreaker",
                paymentServiceCircuitBreakerConfig()
        );
        logCircuitBreakerEvents(circuitBreaker);
        return circuitBreaker;
    }

    // ==================== Retry Configuration ====================

    /**
     * Configure Retry for Product Service
     * - Max attempts: 3
     * - Exponential backoff with initial delay of 500ms and multiplier of 1.5
     */
    @Bean
    public RetryConfig productServiceRetryConfig() {
        log.info("Configuring Retry for Product Service");
        return RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(io.github.resilience4j.core.IntervalFunction.ofExponentialBackoff(500, 1.5))
                .build();
    }

    /**
     * Configure Retry for Payment Service
     * - Max attempts: 2 (more conservative than Product Service)
     * - Exponential backoff with initial delay of 1000ms and multiplier of 1.5
     */
    @Bean
    public RetryConfig paymentServiceRetryConfig() {
        log.info("Configuring Retry for Payment Service");
        return RetryConfig.custom()
                .maxAttempts(2)
                .intervalFunction(io.github.resilience4j.core.IntervalFunction.ofExponentialBackoff(1000, 1.5))
                .build();
    }

    /**
     * Create Retry for Product Service
     */
    @Bean
    public Retry productServiceRetry(RetryRegistry retryRegistry) {
        Retry retry = retryRegistry.retry("productServiceRetry", productServiceRetryConfig());
        logRetryEvents(retry);
        return retry;
    }

    /**
     * Create Retry for Payment Service
     */
    @Bean
    public Retry paymentServiceRetry(RetryRegistry retryRegistry) {
        Retry retry = retryRegistry.retry("paymentServiceRetry", paymentServiceRetryConfig());
        logRetryEvents(retry);
        return retry;
    }

    // ==================== Time Limiter Configuration ====================

    /**
     * Configure TimeLimiter for Product Service
     */
    @Bean
    public TimeLimiterConfig productServiceTimeLimiterConfig() {
        log.info("Configuring TimeLimiter for Product Service");
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5))
                .cancelRunningFuture(true)
                .build();
    }

    /**
     * Configure TimeLimiter for Payment Service
     */
    @Bean
    public TimeLimiterConfig paymentServiceTimeLimiterConfig() {
        log.info("Configuring TimeLimiter for Payment Service");
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(10))
                .cancelRunningFuture(true)
                .build();
    }

    /**
     * Create TimeLimiter for Product Service
     */
    @Bean
    public TimeLimiter productServiceTimeLimiter(TimeLimiterRegistry timeLimiterRegistry) {
        return timeLimiterRegistry.timeLimiter("productServiceTimeLimiter", productServiceTimeLimiterConfig());
    }

    /**
     * Create TimeLimiter for Payment Service
     */
    @Bean
    public TimeLimiter paymentServiceTimeLimiter(TimeLimiterRegistry timeLimiterRegistry) {
        return timeLimiterRegistry.timeLimiter("paymentServiceTimeLimiter", paymentServiceTimeLimiterConfig());
    }

    // ==================== WebClient Configuration ====================

    /**
     * Configure WebClient Builder
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        log.info("Configuring WebClient Builder");
        return WebClient.builder();
    }

    /**
     * Configure WebClient for Product Service
     */
    @Bean
    public WebClient productServiceWebClient(WebClient.Builder builder) {
        log.info("Configuring WebClient for Product Service");
        return builder
                .baseUrl("http://localhost:8081/api")
                .build();
    }

    /**
     * Configure WebClient for Payment Service
     */
    @Bean
    public WebClient paymentServiceWebClient(WebClient.Builder builder) {
        log.info("Configuring WebClient for Payment Service");
        return builder
                .baseUrl("http://localhost:8082/api")
                .build();
    }

    // ==================== Helper Methods ====================

    /**
     * Log Circuit Breaker events
     */
    private void logCircuitBreakerEvents(CircuitBreaker circuitBreaker) {
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> log.warn("Circuit Breaker {} state changed from {} to {}",
                        circuitBreaker.getName(),
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()))
                .onError(event -> log.error("Circuit Breaker {} recorded error: {}",
                        circuitBreaker.getName(),
                        event.getThrowable().getClass().getSimpleName()))
                .onSuccess(event -> log.debug("Circuit Breaker {} recorded success", circuitBreaker.getName()));
    }

    /**
     * Log Retry events
     */
    private void logRetryEvents(Retry retry) {
        retry.getEventPublisher()
                .onSuccess(event -> log.debug("Retry {} succeeded", retry.getName()))
                .onError(event -> {
                    Throwable throwable = event.getLastThrowable();
                    log.error("Retry {} failed. Exception: {}",
                            retry.getName(),
                            throwable != null ? throwable.getClass().getSimpleName() : "Unknown");
                });
    }
}

