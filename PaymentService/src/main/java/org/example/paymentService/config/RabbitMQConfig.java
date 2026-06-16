package org.example.paymentService.config;

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
    public static final String PAYMENT_REQUEST_QUEUE = "payment.request.queue";
    public static final String PAYMENT_RESULT_ROUTING_KEY = "payment.result";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue paymentRequestQueue() {
        return org.springframework.amqp.core.QueueBuilder.durable(PAYMENT_REQUEST_QUEUE)
                .build();
    }

    @Bean
    public Binding bindingPaymentRequest(
            @org.springframework.beans.factory.annotation.Qualifier("paymentRequestQueue") Queue paymentRequestQueue,
            @org.springframework.beans.factory.annotation.Qualifier("exchange") TopicExchange exchange) {
        return BindingBuilder.bind(paymentRequestQueue).to(exchange).with("payment.request");
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
