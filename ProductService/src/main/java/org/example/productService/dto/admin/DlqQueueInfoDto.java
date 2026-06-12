package org.example.productService.dto.admin;
public record DlqQueueInfoDto(String queueName, long messageCount, String retryExchange, String retryRoutingKey) {}
