package org.example.orderService.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
public class InternalTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(InternalTokenProvider.class);
    private final WebClient webClient;

    private String cachedToken;
    private long tokenExpiryTime;

    public InternalTokenProvider(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("lb://user-service").build();
    }

    public synchronized String getToken() {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            return cachedToken;
        }

        log.info("Fetching new internal service token from user-service...");
        try {
            String clientId = "internal-service";
            String clientSecret = "secret";
            String credentials = clientId + ":" + clientSecret;
            String base64Credentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri("/api/auth/client-token")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + base64Credentials)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("access_token")) {
                this.cachedToken = (String) response.get("access_token");
                Integer expiresIn = (Integer) response.get("expires_in");
                this.tokenExpiryTime = System.currentTimeMillis() + ((expiresIn - 60) * 1000L);
                log.info("Successfully fetched new internal service token");
                return cachedToken;
            }
        } catch (Exception e) {
            log.error("Failed to fetch internal service token", e);
        }

        throw new RuntimeException("Could not fetch internal service token");
    }
}
