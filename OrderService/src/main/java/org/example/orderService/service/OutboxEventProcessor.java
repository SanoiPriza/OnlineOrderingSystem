package org.example.orderService.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.example.common.event.OrderCreatedEvent;
import org.example.common.event.StockCompensationEvent;
import org.example.common.event.PaymentRequestEvent;
import org.example.orderService.config.RabbitMQConfig;
import org.example.orderService.model.OutboxEvent;
import org.example.orderService.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.scheduling.annotation.Async;
import org.example.orderService.event.OutboxSavedEvent;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Component
public class OutboxEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventProcessor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final long STUCK_PROCESSING_MINUTES = 10;

    public static final int CLEANUP_RETENTION_DAYS = 7;

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final OrderPaymentProcessor orderPaymentProcessor;

    public OutboxEventProcessor(OutboxEventRepository outboxEventRepository,
            RabbitTemplate rabbitTemplate,
            OrderPaymentProcessor orderPaymentProcessor) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.orderPaymentProcessor = orderPaymentProcessor;
    }

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private OutboxEventProcessor self;

    @Scheduled(fixedDelay = 60000)
    @SchedulerLock(name = "OutboxEventProcessor_processOutboxEvents", lockAtLeastFor = "4s", lockAtMostFor = "10s")
    @Transactional
    public void processOutboxEvents() {
        recoverStuckEvents();

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

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutboxSaved(OutboxSavedEvent event) {
        log.info("OutboxSavedEvent received, triggering outbox processing");
        self.processOutboxEvents();
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

    private void recoverStuckEvents() {
        LocalDateTime threshold = LocalDateTime.now().minus(STUCK_PROCESSING_MINUTES, ChronoUnit.MINUTES);
        List<OutboxEvent> stuckEvents = outboxEventRepository.findStuckProcessingEvents(threshold);
        if (!stuckEvents.isEmpty()) {
            for (OutboxEvent stuck : stuckEvents) {
                stuck.setStatus(OutboxEvent.EventStatus.PENDING);
                outboxEventRepository.save(stuck);
                log.warn("Recovered stuck PROCESSING event id={} type={} orderId={} (created={}). "
                        + "Resetting to PENDING for retry.",
                        stuck.getId(), stuck.getEventType(), stuck.getOrderId(), stuck.getCreatedAt());
            }
        }
    }

    private void publishEvent(OutboxEvent event) throws Exception {
        if (event.getEventType() == OutboxEvent.EventType.INITIATE_PAYMENT) {
            orderPaymentProcessor.publishPaymentRequestEvent(event.getOrderId());
            return;
        }
        JsonNode payload = objectMapper.readTree(event.getPayload());
        String productId = payload.has("productId") ? payload.get("productId").asText() : null;
        int quantity = payload.has("quantity") ? payload.get("quantity").asInt() : 0;

        if (event.getEventType() == OutboxEvent.EventType.ORDER_CREATED) {
            OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(
                    event.getId().toString(), event.getOrderId().toString(), productId, quantity);
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ORDER_CREATED_ROUTING_KEY, orderCreatedEvent);
        } else if (event.getEventType() == OutboxEvent.EventType.STOCK_COMPENSATION
                || event.getEventType() == OutboxEvent.EventType.STOCK_RESTORE) {
            StockCompensationEvent compensationEvent = new StockCompensationEvent(
                    event.getId().toString(), event.getOrderId().toString(), productId, quantity);
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.STOCK_COMPENSATION_ROUTING_KEY, compensationEvent);
        }
    }
}
