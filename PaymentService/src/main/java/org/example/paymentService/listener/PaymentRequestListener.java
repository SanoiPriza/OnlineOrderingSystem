package org.example.paymentService.listener;

import org.example.common.event.PaymentRequestEvent;
import org.example.paymentService.config.RabbitMQConfig;
import org.example.paymentService.model.PaymentRequest;
import org.example.paymentService.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentRequestListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentRequestListener.class);

    private final PaymentService paymentService;

    public PaymentRequestListener(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_REQUEST_QUEUE)
    public void handlePaymentRequest(PaymentRequestEvent event) {
        log.info("Received PaymentRequestEvent for order {}", event.getOrderId());

        PaymentRequest request = new PaymentRequest();
        request.setEventId(event.getEventId());
        request.setOrderId(event.getOrderId());
        request.setAmount(event.getAmount());
        request.setCurrency(event.getCurrency());
        request.setPaymentMethod(event.getPaymentMethod());

        paymentService.processPaymentAsync(request);
        log.info("Started async payment process for order {}", event.getOrderId());
    }
}
