package org.example.orderService.dto.admin;

import java.util.Map;

public record OrderStatsDto(Map<String, Long> countByStatus, long total) {}
