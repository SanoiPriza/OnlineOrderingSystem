package org.example.adminService.service;

import org.example.adminService.config.RabbitMQConfig;
import org.example.adminService.dto.QueueStats;
import org.example.adminService.dto.RetryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class DlqService {

    private static final Logger log = LoggerFactory.getLogger(DlqService.class);

    private static final Map<String, String> DLQ_TO_ROUTING_KEY = Map.of(
            RabbitMQConfig.ORDER_CREATED_DLQ,            RabbitMQConfig.ORDER_CREATED_RK,
            RabbitMQConfig.STOCK_COMPENSATION_DLQ,       RabbitMQConfig.STOCK_COMPENSATION_RK,
            RabbitMQConfig.STOCK_RESERVED_DLQ,           RabbitMQConfig.STOCK_RESERVED_RK,
            RabbitMQConfig.STOCK_RESERVATION_FAILED_DLQ, RabbitMQConfig.STOCK_RESERVATION_FAILED_RK
    );

    private final RabbitTemplate rabbitTemplate;
    private final RestClient      mgmtClient;

    public DlqService(
            RabbitTemplate rabbitTemplate,
            @Value("${rabbitmq.management.url}") String mgmtUrl,
            @Value("${rabbitmq.management.username}") String mgmtUser,
            @Value("${rabbitmq.management.password}") String mgmtPass) {

        this.rabbitTemplate = rabbitTemplate;
        String credentials = Base64.getEncoder()
                .encodeToString((mgmtUser + ":" + mgmtPass).getBytes(StandardCharsets.UTF_8));
        this.mgmtClient = RestClient.builder()
                .baseUrl(mgmtUrl)
                .defaultHeader("Authorization", "Basic " + credentials)
                .build();
    }

    public List<QueueStats> getAllQueueStats() {
        try {
            List<Map<String, Object>> apiQueues = mgmtClient.get()
                    .uri("/api/queues/{vhost}", "/")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            if (apiQueues == null) return List.of();

            List<QueueStats> result = new ArrayList<>();
            for (Map<String, Object> q : apiQueues) {
                String name  = (String) q.get("name");
                long msgs    = toLong(q.get("messages"));
                long ready   = toLong(q.get("messages_ready"));
                long unacked = toLong(q.get("messages_unacknowledged"));
                String state = (String) q.getOrDefault("state", "unknown");
                boolean isDlq = name != null && name.endsWith(".dlq");
                result.add(new QueueStats(name, msgs, ready, unacked, state, isDlq));
            }
            return result;
        } catch (Exception e) {
            log.warn("Could not reach RabbitMQ Management API: {}", e.getMessage());
            return List.of();
        }
    }

    public RetryResult retryAllFromDlq(String dlqName) {
        String routingKey = DLQ_TO_ROUTING_KEY.get(dlqName);
        if (routingKey == null) {
            return new RetryResult(dlqName, 0,
                    "Unknown DLQ '" + dlqName + "'. No routing key mapping found.");
        }

        int requeued = 0;
        try {
            Message message;
            while ((message = rabbitTemplate.receive(dlqName, 200)) != null) {
                rabbitTemplate.send(RabbitMQConfig.MAIN_EXCHANGE, routingKey, message);
                requeued++;
            }
            log.info("DLQ retry: moved {} messages from '{}' → exchange '{}' rk '{}'",
                    requeued, dlqName, RabbitMQConfig.MAIN_EXCHANGE, routingKey);
            return new RetryResult(dlqName, requeued,
                    requeued == 0 ? "DLQ was empty" : "Requeued " + requeued + " messages");
        } catch (Exception e) {
            log.error("DLQ retry failed for '{}' after {} messages: {}", dlqName, requeued, e.getMessage(), e);
            return new RetryResult(dlqName, requeued,
                    "Error after " + requeued + " messages: " + e.getMessage());
        }
    }

    public List<String> knownDlqNames() {
        return new ArrayList<>(DLQ_TO_ROUTING_KEY.keySet());
    }

    private static long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        return 0L;
    }
}