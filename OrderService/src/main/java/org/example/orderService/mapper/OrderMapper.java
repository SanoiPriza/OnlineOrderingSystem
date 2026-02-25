package org.example.orderService.mapper;

import org.example.orderService.dto.OrderRequest;
import org.example.orderService.dto.OrderResponse;
import org.example.orderService.model.OrderEntity;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderEntity toEntity(OrderRequest request) {
        if (request == null) {
            return null;
        }

        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setCustomerName(request.getCustomerName());
        orderEntity.setProductId(request.getProductId());
        orderEntity.setQuantity(request.getQuantity());
        orderEntity.setTotalPrice(request.getTotalPrice());
        orderEntity.setPaymentMethod(request.getPaymentMethod());

        return orderEntity;
    }

    public OrderResponse toResponse(OrderEntity entity) {
        if (entity == null) {
            return null;
        }

        return OrderResponse.builder()
                .id(entity.getId())
                .customerName(entity.getCustomerName())
                .productId(entity.getProductId())
                .quantity(entity.getQuantity())
                .totalPrice(entity.getTotalPrice())
                .status(entity.getStatus())
                .statusMessage(entity.getStatusMessage())
                .paymentMethod(entity.getPaymentMethod())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .paymentTransactionId(entity.getPaymentTransactionId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
