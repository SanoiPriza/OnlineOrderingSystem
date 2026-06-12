package org.example.orderService.controller;

import org.example.orderService.dto.OutboxStats;
import org.example.orderService.model.OutboxEvent;
import org.example.orderService.repository.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/admin")
public class AdminStatsController {

    private static final long STUCK_MINUTES = 10;

    private final OutboxEventRepository outboxRepo;
    private final String internalToken;

    public AdminStatsController(OutboxEventRepository outboxRepo,
            @Value("${internal.service.token}") String internalToken) {
        this.outboxRepo    = outboxRepo;
        this.internalToken = internalToken;
    }

    @GetMapping("/outbox/stats")
    public ResponseEntity<OutboxStats> outboxStats(
            @RequestHeader(value = "X-Internal-Service-Token", required = false) String token) {
        if (!internalToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        LocalDateTime threshold = LocalDateTime.now().minus(STUCK_MINUTES, ChronoUnit.MINUTES);
        return ResponseEntity.ok(new OutboxStats(
                outboxRepo.countByStatus(OutboxEvent.EventStatus.PENDING),
                outboxRepo.countByStatus(OutboxEvent.EventStatus.PROCESSING),
                outboxRepo.countByStatus(OutboxEvent.EventStatus.COMPLETED),
                outboxRepo.countByStatus(OutboxEvent.EventStatus.FAILED),
                outboxRepo.countStuckProcessingEvents(threshold)));
    }
}
