package org.example.orderService.listener;

import org.example.common.event.StockReservationFailedEvent;
import org.example.common.event.StockReservedEvent;
import org.example.common.model.OrderStatus;
import org.example.orderService.config.RabbitMQConfig;
import org.example.orderService.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.example.orderService.repository.ProcessedEventRepository;
import org.example.orderService.model.ProcessedEvent;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final OrderService orderService;
    private final ProcessedEventRepository processedEventRepository;

    public OrderEventListener(OrderService orderService, ProcessedEventRepository processedEventRepository) {
        this.orderService = orderService;
        this.processedEventRepository = processedEventRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.STOCK_RESERVED_QUEUE)
    public void handleStockReservedEvent(StockReservedEvent event) {
        log.info("Received StockReservedEvent (eventId: {}) for order ID: {}", event.getEventId(), event.getOrderId());

        try {
            orderService.processStockReserved(event);
            log.info("Order {} status updated to STOCK_RESERVED and INITIATE_PAYMENT outbox event created.", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to process StockReservedEvent for order ID: {}", event.getOrderId(), e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.STOCK_RESERVATION_FAILED_QUEUE)
    public void handleStockReservationFailedEvent(StockReservationFailedEvent event) {
        log.warn("Received StockReservationFailedEvent (eventId: {}) for order ID: {}. Reason: {}", event.getEventId(),
                event.getOrderId(),
                event.getReason());

        if (event.getEventId() != null && processedEventRepository.existsById(event.getEventId())) {
            log.info("Event {} already processed. Skipping.", event.getEventId());
            return;
        }

        try {
            Long orderId = Long.parseLong(event.getOrderId());
            orderService.updateOrderStatus(orderId, OrderStatus.FAILED);

            if (event.getEventId() != null) {
                processedEventRepository.save(new ProcessedEvent(event.getEventId()));
            }

            log.info("Order {} status updated to FAILED due to stock reservation failure.", orderId);
        } catch (Exception e) {
            log.error("Failed to process StockReservationFailedEvent for order ID: {}", event.getOrderId(), e);
        }
    }
}
