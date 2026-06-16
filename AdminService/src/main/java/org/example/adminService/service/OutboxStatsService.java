package org.example.adminService.service;

import org.example.adminService.dto.OutboxStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import org.example.common.security.jwt.JwtTokenUtil;
import org.springframework.security.core.userdetails.User;

@Service
public class OutboxStatsService {

    private static final Logger   log     = LoggerFactory.getLogger(OutboxStatsService.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final RestClient restClient;

    public OutboxStatsService(
            @Value("${order-service.url}") String orderServiceUrl,
            @Value("${internal.service.token}") String internalToken,
            JwtTokenUtil jwtTokenUtil) {

        String adminToken = jwtTokenUtil.generateToken(
                User.withUsername("admin")
                        .password("")
                        .authorities("ROLE_ADMIN")
                        .build()
        );

        this.restClient = RestClient.builder()
                .baseUrl(orderServiceUrl)
                .defaultHeader("X-Internal-Service-Token", internalToken)
                .defaultHeader("Authorization", "Bearer " + adminToken)
                .build();
    }

    public OutboxStats fetch() {
        try {
            OutboxStats stats = restClient.get()
                    .uri("/api/admin/outbox/stats")
                    .retrieve()
                    .body(OutboxStats.class);
            return stats != null ? stats : emptyStats();
        } catch (Exception e) {
            log.warn("Could not fetch outbox stats from OrderService: {}", e.getMessage());
            return emptyStats();
        }
    }

    private OutboxStats emptyStats() {
        return new OutboxStats(-1, -1, -1, -1);
    }
}
