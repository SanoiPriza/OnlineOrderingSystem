package org.example.productService.config;

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

    public static final String STOCK_RESERVED_ROUTING_KEY = "stock.reserved";
    public static final String STOCK_RESERVATION_FAILED_ROUTING_KEY = "stock.reservation.failed";

    public static final String ORDER_CREATED_QUEUE = "order.created.queue";
    public static final String STOCK_COMPENSATION_QUEUE = "stock.compensation.queue";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return new Queue(ORDER_CREATED_QUEUE);
    }

    @Bean
    public Queue stockCompensationQueue() {
        return new Queue(STOCK_COMPENSATION_QUEUE);
    }

    @Bean
    public Binding bindingOrderCreated(Queue orderCreatedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(exchange).with("order.created");
    }

    @Bean
    public Binding bindingStockCompensation(Queue stockCompensationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(stockCompensationQueue).to(exchange).with("stock.compensation");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
