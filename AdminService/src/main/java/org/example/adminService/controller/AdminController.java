package org.example.adminService.controller;

import org.example.adminService.dto.DashboardSnapshot;
import org.example.adminService.dto.RetryResult;
import org.example.adminService.service.DlqService;
import org.example.adminService.service.HealthCheckService;
import org.example.adminService.service.OutboxStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final HealthCheckService healthCheckService;
    private final DlqService         dlqService;
    private final OutboxStatsService outboxStatsService;

    public AdminController(HealthCheckService healthCheckService,
                           DlqService dlqService,
                           OutboxStatsService outboxStatsService) {
        this.healthCheckService = healthCheckService;
        this.dlqService         = dlqService;
        this.outboxStatsService = outboxStatsService;
    }

    @GetMapping("/snapshot")
    public ResponseEntity<DashboardSnapshot> snapshot() {
        DashboardSnapshot snap = new DashboardSnapshot();
        snap.setServices(healthCheckService.checkAll());
        snap.setQueues(dlqService.getAllQueueStats());
        snap.setKnownDlqNames(dlqService.knownDlqNames());
        snap.setOutbox(outboxStatsService.fetch());
        snap.setSnapshotTime(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return ResponseEntity.ok(snap);
    }

    @PostMapping("/dlq/{queue}/retry")
    public ResponseEntity<RetryResult> retryDlq(@PathVariable("queue") String queue) {
        return ResponseEntity.ok(dlqService.retryAllFromDlq(queue));
    }
}
