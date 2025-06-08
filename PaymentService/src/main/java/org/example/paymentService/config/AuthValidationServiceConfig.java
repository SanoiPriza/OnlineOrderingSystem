package org.example.paymentService.config;

import org.example.common.security.service.AuthValidationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AuthValidationServiceConfig {

    @Value("${user-service.url}")
    private String userServiceUrl;

    @Bean
    public AuthValidationService authValidationService(RestTemplate restTemplate) {
        return new AuthValidationService(restTemplate, userServiceUrl);
    }
}