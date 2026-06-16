package org.example.adminService.service;

import org.example.adminService.dto.ServiceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.example.common.security.jwt.JwtTokenUtil;
import org.springframework.security.core.userdetails.User;

@Service
public class HealthCheckService {

    private static final Logger   log     = LoggerFactory.getLogger(HealthCheckService.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final RestClient restClient;

    private final Map<String, String> serviceUrls;

    public HealthCheckService(
            @Value("${service.urls.user-service}")      String userUrl,
            @Value("${service.urls.order-service}")     String orderUrl,
            @Value("${service.urls.payment-service}")   String paymentUrl,
            @Value("${service.urls.product-service}")   String productUrl,
            @Value("${service.urls.discovery-service}") String discoveryUrl,
            @Value("${service.urls.api-gateway}")       String apiGatewayUrl,
            @Value("${service.urls.admin-service}")     String adminUrl,
            JwtTokenUtil jwtTokenUtil) {

        String adminToken = jwtTokenUtil.generateToken(
                User.withUsername("admin")
                        .password("")
                        .authorities("ROLE_ADMIN")
                        .build()
        );

        this.restClient = RestClient.builder()
                .defaultHeader("Authorization", "Bearer " + adminToken)
                .build();
        this.serviceUrls = Map.of(
                "UserService",      userUrl,
                "OrderService",     orderUrl,
                "PaymentService",   paymentUrl,
                "ProductService",   productUrl,
                "DiscoveryService", discoveryUrl,
                "ApiGateway",       apiGatewayUrl,
                "AdminService",     adminUrl
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
            Map<String, Object> body = restClient.get()
                    .uri(baseUrl + "/actuator/health")
                    .retrieve()
                    .body(Map.class);

            String status = body != null ? (String) body.getOrDefault("status", "UNKNOWN") : "UNKNOWN";
            return new ServiceStatus(name, baseUrl, status, null);
        } catch (Exception e) {
            log.debug("Health check failed for {} @ {}: {}", name, baseUrl, e.getMessage());
            return new ServiceStatus(name, baseUrl, "DOWN", e.getMessage());
        }
    }
}
