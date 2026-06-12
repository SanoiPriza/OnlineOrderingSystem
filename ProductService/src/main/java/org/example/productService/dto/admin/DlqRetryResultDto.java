package org.example.productService.dto.admin;
public record DlqRetryResultDto(String queueName, int retriedCount) {}
