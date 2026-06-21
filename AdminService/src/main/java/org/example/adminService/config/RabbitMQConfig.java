package org.example.adminService.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String MAIN_EXCHANGE = "ordering.exchange";
    public static final String DLX_EXCHANGE = "ordering.dlx";

    public static final String ORDER_CREATED_DLQ = "order.created.dlq";
    public static final String STOCK_COMPENSATION_DLQ = "stock.compensation.dlq";
    public static final String STOCK_RESERVED_DLQ = "stock.reserved.dlq";
    public static final String STOCK_RESERVATION_FAILED_DLQ = "stock.reservation.failed.dlq";

    public static final String ORDER_CREATED_RK = "order.created";
    public static final String STOCK_COMPENSATION_RK = "stock.compensation";
    public static final String STOCK_RESERVED_RK = "stock.reserved";
    public static final String STOCK_RESERVATION_FAILED_RK = "stock.reservation.failed";

    @Bean
    public TopicExchange mainExchange() {
        return new TopicExchange(MAIN_EXCHANGE);
    }

    @Bean
    public TopicExchange dlxExchange() {
        return new TopicExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue orderCreatedDlq() {
        return new Queue(ORDER_CREATED_DLQ, true);
    }

    @Bean
    public Queue stockCompensationDlq() {
        return new Queue(STOCK_COMPENSATION_DLQ, true);
    }

    @Bean
    public Queue stockReservedDlq() {
        return new Queue(STOCK_RESERVED_DLQ, true);
    }

    @Bean
    public Queue stockReservationFailedDlq() {
        return new Queue(STOCK_RESERVATION_FAILED_DLQ, true);
    }

    @Bean
    public Binding bOrderCreatedDlq(@Qualifier("orderCreatedDlq") Queue orderCreatedDlq,
                                    @Qualifier("dlxExchange") TopicExchange dlxExchange) {
        return BindingBuilder.bind(orderCreatedDlq).to(dlxExchange).with("order.created.queue.dlq");
    }

    @Bean
    public Binding bStockCompensationDlq(@Qualifier("stockCompensationDlq") Queue stockCompensationDlq,
                                         @Qualifier("dlxExchange") TopicExchange dlxExchange) {
        return BindingBuilder.bind(stockCompensationDlq).to(dlxExchange).with("stock.compensation.queue.dlq");
    }

    @Bean
    public Binding bStockReservedDlq(@Qualifier("stockReservedDlq") Queue stockReservedDlq,
                                     @Qualifier("dlxExchange") TopicExchange dlxExchange) {
        return BindingBuilder.bind(stockReservedDlq).to(dlxExchange).with("stock.reserved.queue.dlq");
    }

    @Bean
    public Binding bStockReservationFailedDlq(@Qualifier("stockReservationFailedDlq") Queue stockReservationFailedDlq,
                                              @Qualifier("dlxExchange") TopicExchange dlxExchange) {
        return BindingBuilder.bind(stockReservationFailedDlq).to(dlxExchange)
                .with("stock.reservation.failed.queue.dlq");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
