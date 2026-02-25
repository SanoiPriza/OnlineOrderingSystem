package org.example.orderService.listener;

import org.example.common.event.StockReservationFailedEvent;
import org.example.common.event.StockReservedEvent;
import org.example.common.model.OrderStatus;
import org.example.orderService.config.RabbitMQConfig;
import org.example.orderService.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final OrderService orderService;

    public OrderEventListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = RabbitMQConfig.STOCK_RESERVED_QUEUE)
    public void handleStockReservedEvent(StockReservedEvent event) {
        log.info("Received StockReservedEvent for order ID: {}", event.getOrderId());
        try {
            Long orderId = Long.parseLong(event.getOrderId());
            orderService.updateOrderStatus(orderId, OrderStatus.STOCK_RESERVED);
            log.info("Order {} status updated to STOCK_RESERVED. Triggering async payment...", orderId);
            orderService.processOrderPaymentAsync(orderId);
        } catch (Exception e) {
            log.error("Failed to process StockReservedEvent for order ID: {}", event.getOrderId(), e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.STOCK_RESERVATION_FAILED_QUEUE)
    public void handleStockReservationFailedEvent(StockReservationFailedEvent event) {
        log.warn("Received StockReservationFailedEvent for order ID: {}. Reason: {}", event.getOrderId(),
                event.getReason());
        try {
            Long orderId = Long.parseLong(event.getOrderId());
            orderService.updateOrderStatus(orderId, OrderStatus.FAILED);
            log.info("Order {} status updated to FAILED due to stock reservation failure.", orderId);
        } catch (Exception e) {
            log.error("Failed to process StockReservationFailedEvent for order ID: {}", event.getOrderId(), e);
        }
    }
}
