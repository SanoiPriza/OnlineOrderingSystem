package org.example.orderService.service;

import org.example.orderService.dto.admin.DlqMessageInfoDto;
import org.example.orderService.dto.admin.DlqQueueInfoDto;
import org.example.orderService.dto.admin.DlqRetryResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Service
public class DlqAdminService {

    private static final Logger log = LoggerFactory.getLogger(DlqAdminService.class);

    private static final Map<String, String[]> DLQ_ROUTING = Map.of(
            "stock.reserved.dlq",
            new String[] { "ordering.exchange", "stock.reserved" },
            "stock.reservation.failed.dlq",
            new String[] { "ordering.exchange", "stock.reservation.failed" });

    private final RabbitTemplate rabbitTemplate;
    private final RabbitAdmin rabbitAdmin;

    public DlqAdminService(RabbitTemplate rabbitTemplate, RabbitAdmin rabbitAdmin) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitAdmin = rabbitAdmin;
    }

    public List<DlqQueueInfoDto> getAllDlqInfo() {
        List<DlqQueueInfoDto> result = new ArrayList<>();
        for (Map.Entry<String, String[]> entry : DLQ_ROUTING.entrySet()) {
            String queueName = entry.getKey();
            String[] routing = entry.getValue();
            long count = getMessageCount(queueName);
            result.add(new DlqQueueInfoDto(queueName, count, routing[0], routing[1]));
        }
        return result;
    }

    public List<DlqMessageInfoDto> peekMessages(String queueName, int limit) {
        validateQueue(queueName);

        List<Message> drained = new ArrayList<>();
        List<DlqMessageInfoDto> dtos = new ArrayList<>();

        Message msg;
        while (dtos.size() < limit && (msg = rabbitTemplate.receive(queueName, 0)) != null) {
            drained.add(msg);
            dtos.add(toDto(msg));
        }

        for (Message m : drained) {
            rabbitTemplate.send("", queueName, m);
        }

        log.debug("Peeked {} messages from {} (re-enqueued all).", dtos.size(), queueName);
        return dtos;
    }

    public DlqRetryResultDto retryAll(String queueName) {
        validateQueue(queueName);
        String[] routing = DLQ_ROUTING.get(queueName);

        int count = 0;
        Message msg;
        while ((msg = rabbitTemplate.receive(queueName, 0)) != null) {
            removeDeathHeaders(msg);
            rabbitTemplate.send(routing[0], routing[1], msg);
            count++;
        }

        log.info("Retried {} messages from {} → exchange='{}' routingKey='{}'.",
                count, queueName, routing[0], routing[1]);
        return new DlqRetryResultDto(queueName, count);
    }

    private long getMessageCount(String queueName) {
        try {
            Properties props = rabbitAdmin.getQueueProperties(queueName);
            if (props == null)
                return 0L;
            Object count = props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
            return count instanceof Number n ? n.longValue() : 0L;
        } catch (Exception e) {
            log.warn("Could not retrieve message count for queue '{}': {}", queueName, e.getMessage());
            return -1L;
        }
    }

    private void validateQueue(String queueName) {
        if (!DLQ_ROUTING.containsKey(queueName)) {
            throw new IllegalArgumentException(
                    "Unknown DLQ '" + queueName + "'. Known queues: " + DLQ_ROUTING.keySet());
        }
    }

    private DlqMessageInfoDto toDto(Message msg) {
        String body = new String(msg.getBody(), StandardCharsets.UTF_8);
        String routingKey = msg.getMessageProperties().getReceivedRoutingKey();

        Object xDeath = msg.getMessageProperties().getHeaders().get("x-death");
        int deathCount = 0;
        String reason = "unknown";
        if (xDeath instanceof List<?> list && !list.isEmpty()) {
            deathCount = list.size();
            Object first = list.get(0);
            if (first instanceof Map<?, ?> m) {
                Object r = m.get("reason");
                if (r != null)
                    reason = r.toString();
            }
        }

        return new DlqMessageInfoDto(body, routingKey, deathCount, reason);
    }

    private void removeDeathHeaders(Message msg) {
        Map<String, Object> headers = msg.getMessageProperties().getHeaders();
        headers.remove("x-death");
        headers.remove("x-first-death-exchange");
        headers.remove("x-first-death-queue");
        headers.remove("x-first-death-reason");
    }
}
