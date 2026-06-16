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

import org.example.productService.repository.ProcessedEventRepository;
import org.example.productService.model.ProcessedEvent;

@Component
public class ProductEventListener {

    private static final Logger log = LoggerFactory.getLogger(ProductEventListener.class);

    private final ProductService productService;
    private final RabbitTemplate rabbitTemplate;
    private final ProcessedEventRepository processedEventRepository;

    public ProductEventListener(ProductService productService, RabbitTemplate rabbitTemplate,
            ProcessedEventRepository processedEventRepository) {
        this.productService = productService;
        this.rabbitTemplate = rabbitTemplate;
        this.processedEventRepository = processedEventRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent (eventId: {}) for order ID: {}", event.getEventId(), event.getOrderId());

        try {
            productService.processOrderCreated(event);

            StockReservedEvent reservedEvent = new StockReservedEvent(java.util.UUID.randomUUID().toString(),
                    event.getOrderId());
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.STOCK_RESERVED_ROUTING_KEY,
                    reservedEvent);
            log.info("Successfully processed OrderCreatedEvent and sent StockReservedEvent for order ID: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to reserve stock for order ID: {}", event.getOrderId(), e);
            StockReservationFailedEvent failedEvent = new StockReservationFailedEvent(
                    java.util.UUID.randomUUID().toString(), event.getOrderId(),
                    e.getMessage());
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.STOCK_RESERVATION_FAILED_ROUTING_KEY, failedEvent);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.STOCK_COMPENSATION_QUEUE)
    public void handleStockCompensationEvent(StockCompensationEvent event) {
        log.info("Received StockCompensationEvent (eventId: {}) for order ID: {}", event.getEventId(),
                event.getOrderId());

        try {
            productService.processStockCompensation(event);

            log.info("Successfully processed StockCompensationEvent for order ID: {}", event.getOrderId());

            log.info("Successfully compensated stock for order ID: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to compensate stock for order ID: {}. MANUAL INTERVENTION REQUIRED.", event.getOrderId(),
                    e);
        }
    }
}
