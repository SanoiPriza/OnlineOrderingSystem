package org.example.productService.listener;

import org.example.common.event.OrderCreatedEvent;
import org.example.common.event.StockCompensationEvent;
import org.example.productService.config.RabbitMQConfig;
import org.example.productService.repository.ProcessedEventRepository;
import org.example.productService.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ProductEventListener {

    private static final Logger log = LoggerFactory.getLogger(ProductEventListener.class);

    private final ProductService productService;
    private final ProcessedEventRepository processedEventRepository;

    public ProductEventListener(ProductService productService,
                                ProcessedEventRepository processedEventRepository) {
        this.productService = productService;
        this.processedEventRepository = processedEventRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent (eventId: {}) for order ID: {}", event.getEventId(), event.getOrderId());

        try {
            productService.processOrderCreated(event);
            log.info("Successfully processed OrderCreatedEvent for order ID: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to reserve stock for order ID: {}", event.getOrderId(), e);
            productService.publishStockReservationFailed(event.getOrderId(), e.getMessage());
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
