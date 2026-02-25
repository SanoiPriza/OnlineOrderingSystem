package org.example.productService.listener;

import org.example.common.event.OrderCreatedEvent;
import org.example.common.event.StockCompensationEvent;
import org.example.common.event.StockReservationFailedEvent;
import org.example.common.event.StockReservedEvent;
import org.example.productService.config.RabbitMQConfig;
import org.example.productService.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductEventListener {

    private static final Logger log = LoggerFactory.getLogger(ProductEventListener.class);

    private final ProductService productService;
    private final RabbitTemplate rabbitTemplate;

    public ProductEventListener(ProductService productService, RabbitTemplate rabbitTemplate) {
        this.productService = productService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for order ID: {}", event.getOrderId());
        try {
            productService.decrementStock(event.getProductId(), event.getQuantity());
            StockReservedEvent reservedEvent = new StockReservedEvent(event.getOrderId());
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.STOCK_RESERVED_ROUTING_KEY,
                    reservedEvent);
            log.info("Successfully reserved stock for order ID: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to reserve stock for order ID: {}", event.getOrderId(), e);
            StockReservationFailedEvent failedEvent = new StockReservationFailedEvent(event.getOrderId(),
                    e.getMessage());
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.STOCK_RESERVATION_FAILED_ROUTING_KEY, failedEvent);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.STOCK_COMPENSATION_QUEUE)
    public void handleStockCompensationEvent(StockCompensationEvent event) {
        log.info("Received StockCompensationEvent for order ID: {}", event.getOrderId());
        try {
            productService.incrementStock(event.getProductId(), event.getQuantity());
            log.info("Successfully compensated stock for order ID: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to compensate stock for order ID: {}. MANUAL INTERVENTION REQUIRED.", event.getOrderId(),
                    e);
        }
    }
}
