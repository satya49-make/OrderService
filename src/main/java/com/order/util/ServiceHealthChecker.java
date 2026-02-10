package com.order.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class ServiceHealthChecker {

    private final RestTemplate restTemplate;

    public ServiceHealthChecker(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Check if a service is available
     */
    public boolean isServiceAvailable(String serviceUrl) {
        try {
            String healthUrl = serviceUrl.substring(0, serviceUrl.lastIndexOf('/')) + "/health";
            log.debug("Checking health of service: {}", healthUrl);

            Object response = restTemplate.getForObject(healthUrl, Object.class);

            if (response != null) {
                log.info("Service is available: {}", serviceUrl);
                return true;
            }
        } catch (Exception e) {
            log.warn("Service health check failed for: {}. Error: {}", serviceUrl, e.getMessage());
        }

        log.error("Service is NOT available: {}", serviceUrl);
        return false;
    }
}

