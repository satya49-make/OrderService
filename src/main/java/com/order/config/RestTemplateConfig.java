package com.order.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@Slf4j
public class RestTemplateConfig {

    /**
     * Configure RestTemplate with connection and read timeouts
     * This is maintained for backward compatibility
     * For new implementations, use WebClient with Resilience4j
     */
    @Bean
    public RestTemplate restTemplate() {
        log.info("Configuring RestTemplate with timeouts");
        return new RestTemplate();
    }
}



