package org.example.productService.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
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

    public static final String DLX_EXCHANGE_NAME = "ordering.dlx";
    public static final String ORDER_CREATED_DLQ = "order.created.dlq";
    public static final String STOCK_COMPENSATION_DLQ = "stock.compensation.dlq";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public TopicExchange dlxExchange() {
        return new TopicExchange(DLX_EXCHANGE_NAME);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return org.springframework.amqp.core.QueueBuilder.durable(ORDER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                .withArgument("x-dead-letter-routing-key", ORDER_CREATED_QUEUE + ".dlq")
                .build();
    }

    @Bean
    public Queue stockCompensationQueue() {
        return org.springframework.amqp.core.QueueBuilder.durable(STOCK_COMPENSATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                .withArgument("x-dead-letter-routing-key", STOCK_COMPENSATION_QUEUE + ".dlq")
                .build();
    }

    @Bean
    public Queue orderCreatedDlq() {
        return new Queue(ORDER_CREATED_DLQ);
    }

    @Bean
    public Queue stockCompensationDlq() {
        return new Queue(STOCK_COMPENSATION_DLQ);
    }

    @Bean
    public Binding bindingOrderCreated(
            @org.springframework.beans.factory.annotation.Qualifier("orderCreatedQueue") Queue orderCreatedQueue,
            @org.springframework.beans.factory.annotation.Qualifier("exchange") TopicExchange exchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(exchange).with("order.created");
    }

    @Bean
    public Binding bindingStockCompensation(
            @org.springframework.beans.factory.annotation.Qualifier("stockCompensationQueue") Queue stockCompensationQueue,
            @org.springframework.beans.factory.annotation.Qualifier("exchange") TopicExchange exchange) {
        return BindingBuilder.bind(stockCompensationQueue).to(exchange).with("stock.compensation");
    }

    @Bean
    public Binding bindingOrderCreatedDlq(
            @org.springframework.beans.factory.annotation.Qualifier("orderCreatedDlq") Queue orderCreatedDlq,
            @org.springframework.beans.factory.annotation.Qualifier("dlxExchange") TopicExchange dlxExchange) {
        return BindingBuilder.bind(orderCreatedDlq).to(dlxExchange).with(ORDER_CREATED_QUEUE + ".dlq");
    }

    @Bean
    public Binding bindingStockCompensationDlq(
            @org.springframework.beans.factory.annotation.Qualifier("stockCompensationDlq") Queue stockCompensationDlq,
            @org.springframework.beans.factory.annotation.Qualifier("dlxExchange") TopicExchange dlxExchange) {
        return BindingBuilder.bind(stockCompensationDlq).to(dlxExchange).with(STOCK_COMPENSATION_QUEUE + ".dlq");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
