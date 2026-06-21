package org.example.paymentService.service;

import org.example.paymentService.model.OutboxEvent;
import org.example.paymentService.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class OutboxEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventProcessor.class);

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxEventProcessor(OutboxEventRepository outboxEventRepository, RabbitTemplate rabbitTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository
                .findByStatusOrderByCreatedAtAsc(OutboxEvent.EventStatus.PENDING, PageRequest.of(0, 100));

        if (pendingEvents.isEmpty()) {
            return;
        }

        for (OutboxEvent event : pendingEvents) {
            processSingleEvent(event);
        }
    }

    @Transactional
    public void processSingleEvent(OutboxEvent event) {
        try {
            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            Message message = new Message(event.getPayload().getBytes(StandardCharsets.UTF_8), props);

            rabbitTemplate.send(event.getExchange(), event.getRoutingKey(), message);

            event.setStatus(OutboxEvent.EventStatus.COMPLETED);
            event.setProcessedAt(LocalDateTime.now());
            log.info("Outbox event {} published successfully.", event.getId());
        } catch (Exception e) {
            event.setRetryCount(event.getRetryCount() + 1);
            event.setErrorMessage(e.getMessage());

            if (event.canRetry()) {
                log.warn("Outbox event {} failed (attempt {}/5): {}", event.getId(), event.getRetryCount(), e.getMessage());
            } else {
                event.setStatus(OutboxEvent.EventStatus.FAILED);
                log.error("Outbox event {} PERMANENTLY FAILED.", event.getId(), e);
            }
        }
        outboxEventRepository.save(event);
    }
}
