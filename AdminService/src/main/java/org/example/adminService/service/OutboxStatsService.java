package org.example.adminService.service;

import org.example.adminService.dto.OutboxStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Service
public class OutboxStatsService {

    private static final Logger   log     = LoggerFactory.getLogger(OutboxStatsService.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public OutboxStatsService(
            WebClient.Builder builder,
            @Value("${order-service.url}") String orderServiceUrl,
            @Value("${internal.service.token}") String internalToken) {

        this.webClient = builder
                .baseUrl(orderServiceUrl)
                .defaultHeader("X-Internal-Service-Token", internalToken)
                .build();
    }

    public OutboxStats fetch() {
        try {
            OutboxStats stats = webClient.get()
                    .uri("/api/admin/outbox/stats")
                    .retrieve()
                    .bodyToMono(OutboxStats.class)
                    .timeout(TIMEOUT)
                    .block();
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
