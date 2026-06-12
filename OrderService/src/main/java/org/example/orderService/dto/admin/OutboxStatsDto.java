package org.example.orderService.dto.admin;

public record OutboxStatsDto(
        long pending,
        long processing,
        long completed,
        long failed,
        long stuckProcessing
) {}
