package org.example.orderService.dto.admin;

public record DlqQueueInfoDto(
        String queueName,
        long messageCount,
        String retryExchange,
        String retryRoutingKey
) {}
