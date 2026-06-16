package org.example.paymentService.listener;

import org.example.common.event.PaymentRequestEvent;
import org.example.common.event.PaymentResultEvent;
import org.example.paymentService.config.RabbitMQConfig;
import org.example.paymentService.model.PaymentRequest;
import org.example.paymentService.model.PaymentResponse;
import org.example.paymentService.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentRequestListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentRequestListener.class);

    private final PaymentService paymentService;
    private final RabbitTemplate rabbitTemplate;

    public PaymentRequestListener(PaymentService paymentService, RabbitTemplate rabbitTemplate) {
        this.paymentService = paymentService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_REQUEST_QUEUE)
    public void handlePaymentRequest(PaymentRequestEvent event) {
        log.info("Received PaymentRequestEvent for order {}", event.getOrderId());
        
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(event.getOrderId());
        request.setAmount(event.getAmount());
        request.setCurrency(event.getCurrency());
        request.setPaymentMethod(event.getPaymentMethod());

        PaymentResponse response = paymentService.processPayment(request);

        PaymentResultEvent resultEvent = new PaymentResultEvent(
                event.getOrderId(),
                response.getStatus(),
                response.getTransactionId(),
                response.getErrorMessage()
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.PAYMENT_RESULT_ROUTING_KEY, resultEvent);
        
        log.info("Published PaymentResultEvent for order {}", event.getOrderId());
    }
}
