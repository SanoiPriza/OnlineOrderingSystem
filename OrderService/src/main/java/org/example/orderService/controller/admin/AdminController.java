package org.example.orderService.controller.admin;

import org.example.orderService.dto.admin.DlqMessageInfoDto;
import org.example.orderService.dto.admin.DlqQueueInfoDto;
import org.example.orderService.dto.admin.DlqRetryResultDto;
import org.example.orderService.dto.admin.OrderStatsDto;
import org.example.orderService.dto.admin.OutboxStatsDto;
import org.example.orderService.model.OutboxEvent;
import org.example.orderService.repository.OrderRepository;
import org.example.orderService.repository.OutboxEventRepository;
import org.example.orderService.service.DlqAdminService;
import org.example.orderService.service.OutboxEventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final OutboxEventRepository outboxEventRepository;
    private final OrderRepository orderRepository;
    private final DlqAdminService dlqAdminService;
    private final OutboxEventProcessor outboxEventProcessor;
    private final String adminToken;

    public AdminController(OutboxEventRepository outboxEventRepository,
                           OrderRepository orderRepository,
                           DlqAdminService dlqAdminService,
                           OutboxEventProcessor outboxEventProcessor,
                           @Value("${internal.service.token}") String adminToken) {
        this.outboxEventRepository = outboxEventRepository;
        this.orderRepository = orderRepository;
        this.dlqAdminService = dlqAdminService;
        this.outboxEventProcessor = outboxEventProcessor;
        this.adminToken = adminToken;
    }

    @GetMapping("/outbox/stats")
    public OutboxStatsDto getOutboxStats(@RequestHeader("X-Admin-Token") String token) {
        requireAdminToken(token);

        Map<OutboxEvent.EventStatus, Long> counts = new EnumMap<>(OutboxEvent.EventStatus.class);
        for (Object[] row : outboxEventRepository.countGroupedByStatus()) {
            counts.put((OutboxEvent.EventStatus) row[0], (Long) row[1]);
        }

        LocalDateTime stuckThreshold = LocalDateTime.now().minus(10, ChronoUnit.MINUTES);
        long stuck = outboxEventRepository.countStuckProcessingEvents(stuckThreshold);

        return new OutboxStatsDto(
                counts.getOrDefault(OutboxEvent.EventStatus.PENDING, 0L),
                counts.getOrDefault(OutboxEvent.EventStatus.PROCESSING, 0L),
                counts.getOrDefault(OutboxEvent.EventStatus.COMPLETED, 0L),
                counts.getOrDefault(OutboxEvent.EventStatus.FAILED, 0L),
                stuck
        );
    }

    @PostMapping("/outbox/cleanup")
    public Map<String, String> triggerCleanup(@RequestHeader("X-Admin-Token") String token) {
        requireAdminToken(token);
        log.info("Manual outbox cleanup triggered via admin API.");
        outboxEventProcessor.weeklyCleanup();
        return Map.of("status", "cleanup triggered",
                      "retentionDays", String.valueOf(OutboxEventProcessor.CLEANUP_RETENTION_DAYS));
    }

    @GetMapping("/orders/stats")
    public OrderStatsDto getOrderStats(@RequestHeader("X-Admin-Token") String token) {
        requireAdminToken(token);

        Map<String, Long> byStatus = new HashMap<>();
        long total = 0;
        for (Object[] row : orderRepository.countGroupedByStatus()) {
            String statusName = row[0].toString();
            Long count = (Long) row[1];
            byStatus.put(statusName, count);
            total += count;
        }

        return new OrderStatsDto(byStatus, total);
    }

    @GetMapping("/dlq")
    public List<DlqQueueInfoDto> listDlqs(@RequestHeader("X-Admin-Token") String token) {
        requireAdminToken(token);
        return dlqAdminService.getAllDlqInfo();
    }

    @GetMapping("/dlq/{queueName}/messages")
    public List<DlqMessageInfoDto> peekMessages(
            @RequestHeader("X-Admin-Token") String token,
            @PathVariable String queueName,
            @RequestParam(defaultValue = "20") int limit) {
        requireAdminToken(token);
        return dlqAdminService.peekMessages(queueName, Math.min(limit, 100));
    }

    @PostMapping("/dlq/{queueName}/retry-all")
    public DlqRetryResultDto retryAll(
            @RequestHeader("X-Admin-Token") String token,
            @PathVariable String queueName) {
        requireAdminToken(token);
        log.info("Admin DLQ retry-all triggered for queue '{}'.", queueName);
        return dlqAdminService.retryAll(queueName);
    }

    private void requireAdminToken(String provided) {
        if (!adminToken.equals(provided)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid admin token");
        }
    }
}
