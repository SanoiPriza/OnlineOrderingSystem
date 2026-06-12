package org.example.productService.service;

import org.example.productService.dto.admin.DlqMessageInfoDto;
import org.example.productService.dto.admin.DlqQueueInfoDto;
import org.example.productService.dto.admin.DlqRetryResultDto;
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
            "order.created.dlq",
            new String[] { "ordering.exchange", "order.created" },
            "stock.compensation.dlq",
            new String[] { "ordering.exchange", "stock.compensation" });

    private final RabbitTemplate rabbitTemplate;
    private final RabbitAdmin rabbitAdmin;

    public DlqAdminService(RabbitTemplate rabbitTemplate, RabbitAdmin rabbitAdmin) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitAdmin = rabbitAdmin;
    }

    public List<DlqQueueInfoDto> getAllDlqInfo() {
        List<DlqQueueInfoDto> result = new ArrayList<>();
        for (Map.Entry<String, String[]> e : DLQ_ROUTING.entrySet()) {
            long count = getMessageCount(e.getKey());
            result.add(new DlqQueueInfoDto(e.getKey(), count, e.getValue()[0], e.getValue()[1]));
        }
        return result;
    }

    public List<DlqMessageInfoDto> peekMessages(String queueName, int limit) {
        validate(queueName);
        List<Message> drained = new ArrayList<>();
        List<DlqMessageInfoDto> dtos = new ArrayList<>();
        Message msg;
        while (dtos.size() < limit && (msg = rabbitTemplate.receive(queueName, 0)) != null) {
            drained.add(msg);
            dtos.add(toDto(msg));
        }
        for (Message m : drained)
            rabbitTemplate.send("", queueName, m);
        return dtos;
    }

    public DlqRetryResultDto retryAll(String queueName) {
        validate(queueName);
        String[] routing = DLQ_ROUTING.get(queueName);
        int count = 0;
        Message msg;
        while ((msg = rabbitTemplate.receive(queueName, 0)) != null) {
            removeDeathHeaders(msg);
            rabbitTemplate.send(routing[0], routing[1], msg);
            count++;
        }
        log.info("Retried {} messages from {} → {}/{}", count, queueName, routing[0], routing[1]);
        return new DlqRetryResultDto(queueName, count);
    }

    private long getMessageCount(String queueName) {
        try {
            Properties p = rabbitAdmin.getQueueProperties(queueName);
            if (p == null)
                return 0L;
            Object c = p.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
            return c instanceof Number n ? n.longValue() : 0L;
        } catch (Exception e) {
            log.warn("Cannot get count for '{}': {}", queueName, e.getMessage());
            return -1L;
        }
    }

    private void validate(String q) {
        if (!DLQ_ROUTING.containsKey(q))
            throw new IllegalArgumentException("Unknown DLQ: " + q);
    }

    private DlqMessageInfoDto toDto(Message msg) {
        String body = new String(msg.getBody(), StandardCharsets.UTF_8);
        String rk = msg.getMessageProperties().getReceivedRoutingKey();
        Object xd = msg.getMessageProperties().getHeaders().get("x-death");
        int count = 0;
        String reason = "unknown";
        if (xd instanceof List<?> l && !l.isEmpty()) {
            count = l.size();
            if (l.get(0) instanceof Map<?, ?> m && m.get("reason") != null)
                reason = m.get("reason").toString();
        }
        return new DlqMessageInfoDto(body, rk, count, reason);
    }

    private void removeDeathHeaders(Message msg) {
        Map<String, Object> h = msg.getMessageProperties().getHeaders();
        h.remove("x-death");
        h.remove("x-first-death-exchange");
        h.remove("x-first-death-queue");
        h.remove("x-first-death-reason");
    }
}
