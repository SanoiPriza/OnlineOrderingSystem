package org.example.common.security.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthValidationService {

    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    public AuthValidationService(RestTemplate restTemplate, 
                               @Value("${user-service.url:http://localhost:8084}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }

    public boolean validateToken(String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Boolean> response = restTemplate.exchange(
                userServiceUrl + "/api/auth/validate",
                HttpMethod.GET,
                entity,
                Boolean.class
            );

            return Boolean.TRUE.equals(response.getBody());
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                userServiceUrl + "/api/auth/username",
                HttpMethod.GET,
                entity,
                String.class
            );

            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }
}