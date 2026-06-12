package org.example.productService.dto.admin;
public record DlqMessageInfoDto(String payloadJson, String originalRoutingKey, int deathCount, String firstDeathReason) {}
