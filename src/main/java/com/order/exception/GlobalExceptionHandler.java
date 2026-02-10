package com.order.exception;

import com.order.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotAvailableException.class)
    public ResponseEntity<ApiResponse<String>> handleProductNotAvailable(ProductNotAvailableException ex, WebRequest request) {
        String message = ex.getMessage();

        // Log the exception with full stack trace for debugging
        if (message.contains("Cannot reach Product Service") || message.contains("unavailable")) {
            log.error("CRITICAL: Product Service is down or unreachable. Error: {}", message, ex);
        } else if (message.contains("Insufficient stock")) {
            log.warn("Product availability issue: {}", message);
        } else {
            log.error("Product not available error: {}", message, ex);
        }

        ApiResponse<String> response = new ApiResponse<>(false, message, null);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ApiResponse<String>> handlePaymentFailed(PaymentFailedException ex, WebRequest request) {
        String message = ex.getMessage();

        // Log payment service failures with high priority
        if (message.contains("Payment Service is unavailable") || message.contains("Cannot reach Payment Service")) {
            log.error("CRITICAL: Payment Service is down or unreachable. This is a critical service outage. Error: {}", message, ex);
        } else if (message.contains("timeout") || message.contains("gateway")) {
            log.error("WARNING: Payment Service timeout or gateway issue. Error: {}", message, ex);
        } else {
            log.error("Payment processing error: {}", message, ex);
        }

        ApiResponse<String> response = new ApiResponse<>(false, message, null);
        return new ResponseEntity<>(response, HttpStatus.PAYMENT_REQUIRED);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleOrderNotFound(OrderNotFoundException ex, WebRequest request) {
        String message = ex.getMessage();
        log.warn("Order not found: {}", message);

        ApiResponse<String> response = new ApiResponse<>(false, message, null);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGlobalException(Exception ex, WebRequest request) {
        String message = ex.getMessage();
        String errorMessage = "An error occurred: " + message;

        // Check if it's a service-related error
        if (message != null && (
                message.contains("Connection refused") ||
                message.contains("connect timed out") ||
                message.contains("UnknownHostException") ||
                message.contains("HttpServerErrorException"))) {
            log.error("SERVICE CONNECTIVITY ERROR: {}. A dependent service may be down.", message, ex);
            errorMessage = "A dependent service is currently unavailable. Please try again later.";
        } else {
            log.error("Unexpected error occurred: {}", message, ex);
        }

        ApiResponse<String> response = new ApiResponse<>(false, errorMessage, null);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

