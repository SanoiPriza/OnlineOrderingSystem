package org.example.adminService.service;

import org.example.adminService.dto.ServiceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class HealthCheckService {

    private static final Logger   log     = LoggerFactory.getLogger(HealthCheckService.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final WebClient webClient;

    private final Map<String, String> serviceUrls;

    public HealthCheckService(
            WebClient.Builder webClientBuilder,
            @Value("${service.urls.user-service}")      String userUrl,
            @Value("${service.urls.order-service}")     String orderUrl,
            @Value("${service.urls.payment-service}")   String paymentUrl,
            @Value("${service.urls.product-service}")   String productUrl,
            @Value("${service.urls.discovery-service}") String discoveryUrl) {

        this.webClient  = webClientBuilder.build();
        this.serviceUrls = Map.of(
                "UserService",      userUrl,
                "OrderService",     orderUrl,
                "PaymentService",   paymentUrl,
                "ProductService",   productUrl,
                "DiscoveryService", discoveryUrl
        );
    }

    public List<ServiceStatus> checkAll() {
        return serviceUrls.entrySet().stream()
                .map(e -> check(e.getKey(), e.getValue()))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private ServiceStatus check(String name, String baseUrl) {
        try {
            Map<String, Object> body = webClient.get()
                    .uri(baseUrl + "/actuator/health")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .block();

            String status = body != null ? (String) body.getOrDefault("status", "UNKNOWN") : "UNKNOWN";
            return new ServiceStatus(name, baseUrl, status, null);
        } catch (Exception e) {
            log.debug("Health check failed for {} @ {}: {}", name, baseUrl, e.getMessage());
            return new ServiceStatus(name, baseUrl, "DOWN", e.getMessage());
        }
    }
}
