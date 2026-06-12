package org.example.orderService.dto.admin;

public record DlqRetryResultDto(String queueName, int retriedCount) {}
