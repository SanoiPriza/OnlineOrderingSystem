package org.example.orderService.dto.admin;

public record DlqMessageInfoDto(
        String payloadJson,
        String originalRoutingKey,
        int deathCount,
        String firstDeathReason
) {}
