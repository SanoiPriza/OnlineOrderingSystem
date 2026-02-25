package org.example.orderService.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.common.event.OrderCreatedEvent;
import org.example.common.event.StockCompensationEvent;
import org.example.orderService.config.RabbitMQConfig;
import org.example.orderService.model.OutboxEvent;
import org.example.orderService.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OutboxEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventProcessor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxEventProcessor(OutboxEventRepository outboxEventRepository,
            RabbitTemplate rabbitTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    @SchedulerLock(name = "OutboxEventProcessor_processOutboxEvents", lockAtLeastFor = "4s", lockAtMostFor = "10s")
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository
                .findByStatusOrderByCreatedAtAsc(OutboxEvent.EventStatus.PENDING);

        for (OutboxEvent event : pendingEvents) {
            try {
                event.setStatus(OutboxEvent.EventStatus.PROCESSING);
                outboxEventRepository.save(event);

                publishEvent(event);

                event.setStatus(OutboxEvent.EventStatus.COMPLETED);
                event.setProcessedAt(LocalDateTime.now());
                outboxEventRepository.save(event);

                log.info("Outbox event {} (type={}) published successfully for order {}",
                        event.getId(), event.getEventType(), event.getOrderId());

            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                event.setErrorMessage(e.getMessage());

                if (event.canRetry()) {
                    event.setStatus(OutboxEvent.EventStatus.PENDING);
                    log.warn("Outbox event {} failed (attempt {}/5) for order {}: {}",
                            event.getId(), event.getRetryCount(), event.getOrderId(), e.getMessage());
                } else {
                    event.setStatus(OutboxEvent.EventStatus.FAILED);
                    log.error("Outbox event {} PERMANENTLY FAILED for order {}. "
                            + "MANUAL INTERVENTION REQUIRED. Payload: {}",
                            event.getId(), event.getOrderId(), event.getPayload(), e);
                }

                outboxEventRepository.save(event);
            }
        }
    }

    private void publishEvent(OutboxEvent event) throws Exception {
        JsonNode payload = objectMapper.readTree(event.getPayload());
        String productId = payload.get("productId").asText();
        int quantity = payload.get("quantity").asInt();

        if (event.getEventType() == OutboxEvent.EventType.ORDER_CREATED) {
            OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(event.getOrderId().toString(), productId,
                    quantity);
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                    orderCreatedEvent);
        } else if (event.getEventType() == OutboxEvent.EventType.STOCK_COMPENSATION
                || event.getEventType() == OutboxEvent.EventType.STOCK_RESTORE) {
            StockCompensationEvent compensationEvent = new StockCompensationEvent(event.getOrderId().toString(),
                    productId, quantity);
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.STOCK_COMPENSATION_ROUTING_KEY,
                    compensationEvent);
        }
    }
}
