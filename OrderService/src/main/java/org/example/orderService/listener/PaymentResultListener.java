package org.example.orderService.listener;

import org.example.common.event.PaymentResultEvent;
import org.example.orderService.config.RabbitMQConfig;
import org.example.orderService.service.OrderPaymentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentResultListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentResultListener.class);

    private final OrderPaymentProcessor orderPaymentProcessor;

    public PaymentResultListener(OrderPaymentProcessor orderPaymentProcessor) {
        this.orderPaymentProcessor = orderPaymentProcessor;
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_RESULT_QUEUE)
    public void handlePaymentResult(PaymentResultEvent event) {
        log.info("Received PaymentResultEvent for order {}: status={}, transactionId={}",
                event.getOrderId(), event.getStatus(), event.getTransactionId());
        
        orderPaymentProcessor.handlePaymentResult(event);
    }
}
