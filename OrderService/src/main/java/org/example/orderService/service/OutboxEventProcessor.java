package org.example.orderService.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.example.orderService.model.OutboxEvent;
import org.example.orderService.repository.OutboxEventRepository;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class OutboxEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventProcessor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final long STUCK_PROCESSING_MINUTES = 10;

    public static final int CLEANUP_RETENTION_DAYS = 7;

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

    public OutboxEventProcessor(OutboxEventRepository outboxEventRepository,
                                RabbitTemplate rabbitTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    @SchedulerLock(name = "OrderService_processOutboxEvents", lockAtLeastFor = "4s", lockAtMostFor = "1m")
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
            publishEvent(event);
            event.setStatus(OutboxEvent.EventStatus.COMPLETED);
            event.setProcessedAt(LocalDateTime.now());
            log.info("Outbox event {} published successfully for order {}",
                    event.getId(), event.getOrderId());
        } catch (Exception e) {
            event.setRetryCount(event.getRetryCount() + 1);
            event.setErrorMessage(e.getMessage());

            if (event.canRetry()) {
                log.warn("Outbox event {} failed (attempt {}/5) for order {}: {}",
                        event.getId(), event.getRetryCount(), event.getOrderId(), e.getMessage());
            } else {
                event.setStatus(OutboxEvent.EventStatus.FAILED);
                log.error("Outbox event {} PERMANENTLY FAILED for order {}. "
                                + "MANUAL INTERVENTION REQUIRED. Payload: {}",
                        event.getId(), event.getOrderId(), event.getPayload(), e);
            }
        }
        outboxEventRepository.save(event);
    }

    @Scheduled(cron = "0 0 3 * * SUN")
    @SchedulerLock(name = "OutboxEventProcessor_weeklyCleanup", lockAtLeastFor = "1m", lockAtMostFor = "10m")
    @Transactional
    public void weeklyCleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(CLEANUP_RETENTION_DAYS);
        List<OutboxEvent.EventStatus> terminal = Arrays.asList(
                OutboxEvent.EventStatus.COMPLETED,
                OutboxEvent.EventStatus.FAILED);
        int deleted = outboxEventRepository.deleteOldTerminalEvents(terminal, cutoff);
        log.info("Weekly outbox cleanup completed: deleted {} terminal events older than {} days (cutoff={}).",
                deleted, CLEANUP_RETENTION_DAYS, cutoff);
    }

    private void publishEvent(OutboxEvent event) throws Exception {
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        Message message = new Message(event.getPayload().getBytes(StandardCharsets.UTF_8), props);
        rabbitTemplate.send(event.getExchange(), event.getRoutingKey(), message);
    }
}
