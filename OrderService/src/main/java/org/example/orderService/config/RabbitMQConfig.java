package org.example.orderService.config;

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

    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String STOCK_COMPENSATION_ROUTING_KEY = "stock.compensation";
    public static final String PAYMENT_REQUEST_ROUTING_KEY = "payment.request";
    public static final String PRODUCT_UPDATED_ROUTING_KEY = "product.updated";

    public static final String STOCK_RESERVED_QUEUE = "stock.reserved.queue";
    public static final String STOCK_RESERVATION_FAILED_QUEUE = "stock.reservation.failed.queue";
    public static final String PAYMENT_RESULT_QUEUE = "payment.result.queue";
    public static final String PRODUCT_UPDATED_QUEUE = "product.updated.queue";

    public static final String DLX_EXCHANGE_NAME = "ordering.dlx";
    public static final String STOCK_RESERVED_DLQ = "stock.reserved.dlq";
    public static final String STOCK_RESERVATION_FAILED_DLQ = "stock.reservation.failed.dlq";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public TopicExchange dlxExchange() {
        return new TopicExchange(DLX_EXCHANGE_NAME);
    }

    @Bean
    public Queue stockReservedQueue() {
        return org.springframework.amqp.core.QueueBuilder.durable(STOCK_RESERVED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                .withArgument("x-dead-letter-routing-key", STOCK_RESERVED_QUEUE + ".dlq")
                .build();
    }

    @Bean
    public Queue stockReservationFailedQueue() {
        return org.springframework.amqp.core.QueueBuilder.durable(STOCK_RESERVATION_FAILED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                .withArgument("x-dead-letter-routing-key", STOCK_RESERVATION_FAILED_QUEUE + ".dlq")
                .build();
    }

    @Bean
    public Queue paymentResultQueue() {
        return org.springframework.amqp.core.QueueBuilder.durable(PAYMENT_RESULT_QUEUE)
                .build();
    }

    @Bean
    public Queue productUpdatedQueue() {
        return org.springframework.amqp.core.QueueBuilder.durable(PRODUCT_UPDATED_QUEUE)
                .build();
    }

    @Bean
    public Queue stockReservedDlq() {
        return new Queue(STOCK_RESERVED_DLQ);
    }

    @Bean
    public Queue stockReservationFailedDlq() {
        return new Queue(STOCK_RESERVATION_FAILED_DLQ);
    }

    @Bean
    public Binding bindingStockReserved(
            @org.springframework.beans.factory.annotation.Qualifier("stockReservedQueue") Queue stockReservedQueue,
            @org.springframework.beans.factory.annotation.Qualifier("exchange") TopicExchange exchange) {
        return BindingBuilder.bind(stockReservedQueue).to(exchange).with("stock.reserved");
    }

    @Bean
    public Binding bindingStockReservationFailed(
            @org.springframework.beans.factory.annotation.Qualifier("stockReservationFailedQueue") Queue stockReservationFailedQueue,
            @org.springframework.beans.factory.annotation.Qualifier("exchange") TopicExchange exchange) {
        return BindingBuilder.bind(stockReservationFailedQueue).to(exchange).with("stock.reservation.failed");
    }

    @Bean
    public Binding bindingPaymentResult(
            @org.springframework.beans.factory.annotation.Qualifier("paymentResultQueue") Queue paymentResultQueue,
            @org.springframework.beans.factory.annotation.Qualifier("exchange") TopicExchange exchange) {
        return BindingBuilder.bind(paymentResultQueue).to(exchange).with("payment.result");
    }

    @Bean
    public Binding bindingProductUpdated(
            @org.springframework.beans.factory.annotation.Qualifier("productUpdatedQueue") Queue productUpdatedQueue,
            @org.springframework.beans.factory.annotation.Qualifier("exchange") TopicExchange exchange) {
        return BindingBuilder.bind(productUpdatedQueue).to(exchange).with("product.updated");
    }

    @Bean
    public Binding bindingStockReservedDlq(
            @org.springframework.beans.factory.annotation.Qualifier("stockReservedDlq") Queue stockReservedDlq,
            @org.springframework.beans.factory.annotation.Qualifier("dlxExchange") TopicExchange dlxExchange) {
        return BindingBuilder.bind(stockReservedDlq).to(dlxExchange).with(STOCK_RESERVED_QUEUE + ".dlq");
    }

    @Bean
    public Binding bindingStockReservationFailedDlq(
            @org.springframework.beans.factory.annotation.Qualifier("stockReservationFailedDlq") Queue stockReservationFailedDlq,
            @org.springframework.beans.factory.annotation.Qualifier("dlxExchange") TopicExchange dlxExchange) {
        return BindingBuilder.bind(stockReservationFailedDlq).to(dlxExchange)
                .with(STOCK_RESERVATION_FAILED_QUEUE + ".dlq");
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
