package org.example.paymentService.listener;

import org.example.common.event.PaymentRefundRequestEvent;
import org.example.common.event.PaymentResultEvent;
import org.example.paymentService.config.RabbitMQConfig;
import org.example.paymentService.model.PaymentResponse;
import org.example.paymentService.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentRefundListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentRefundListener.class);

    private final PaymentService paymentService;
    private final RabbitTemplate rabbitTemplate;

    public PaymentRefundListener(PaymentService paymentService, RabbitTemplate rabbitTemplate) {
        this.paymentService = paymentService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_REFUND_QUEUE)
    public void handleRefundRequest(PaymentRefundRequestEvent event) {
        log.info("Received PaymentRefundRequestEvent for order {}, tx {}", event.getOrderId(), event.getTransactionId());

        PaymentResponse response = paymentService.refundPayment(event.getTransactionId());

        PaymentResultEvent resultEvent = new PaymentResultEvent(
                event.getOrderId(),
                response.getStatus(),
                response.getTransactionId(),
                response.getErrorMessage()
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.PAYMENT_RESULT_ROUTING_KEY, resultEvent);

        log.info("Published refund PaymentResultEvent for order {}", event.getOrderId());
    }
}
