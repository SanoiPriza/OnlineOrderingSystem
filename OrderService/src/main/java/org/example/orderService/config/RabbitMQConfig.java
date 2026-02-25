package org.example.orderService.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "ordering.exchange";

    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String STOCK_COMPENSATION_ROUTING_KEY = "stock.compensation";

    public static final String STOCK_RESERVED_QUEUE = "stock.reserved.queue";
    public static final String STOCK_RESERVATION_FAILED_QUEUE = "stock.reservation.failed.queue";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue stockReservedQueue() {
        return new Queue(STOCK_RESERVED_QUEUE);
    }

    @Bean
    public Queue stockReservationFailedQueue() {
        return new Queue(STOCK_RESERVATION_FAILED_QUEUE);
    }

    @Bean
    public Binding bindingStockReserved(Queue stockReservedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(stockReservedQueue).to(exchange).with("stock.reserved");
    }

    @Bean
    public Binding bindingStockReservationFailed(Queue stockReservationFailedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(stockReservationFailedQueue).to(exchange).with("stock.reservation.failed");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
