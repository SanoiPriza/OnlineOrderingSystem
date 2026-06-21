package org.example.paymentService.service;

import org.example.common.exception.InvalidOperationException;
import org.example.common.exception.ResourceNotFoundException;
import org.example.common.model.PaymentStatus;
import org.example.paymentService.model.Payment;
import org.example.paymentService.model.PaymentRequest;
import org.example.paymentService.model.PaymentResponse;
import org.example.paymentService.repository.PaymentRepository;
import org.example.paymentService.repository.ProcessedEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayClient paymentGatewayClient;

    private final ProcessedEventRepository processedEventRepository;
    private final org.example.paymentService.repository.OutboxEventRepository outboxEventRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    public PaymentService(PaymentRepository paymentRepository, PaymentGatewayClient paymentGatewayClient, ProcessedEventRepository processedEventRepository, org.example.paymentService.repository.OutboxEventRepository outboxEventRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentGatewayClient = paymentGatewayClient;
        this.processedEventRepository = processedEventRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    public void processPaymentAsync(PaymentRequest paymentRequest) {
        if (paymentRequest.getEventId() != null && processedEventRepository.existsById(paymentRequest.getEventId())) {
            return;
        }

        Payment savedPayment = initiatePayment(paymentRequest);

        paymentGatewayClient.processPayment(savedPayment.getTransactionId(), paymentRequest)
                .subscribe(
                        response -> completePayment(savedPayment.getId(), response, null, paymentRequest.getEventId()),
                        error -> completePayment(savedPayment.getId(), null, error, paymentRequest.getEventId())
                );
    }

    @Transactional
    public Payment initiatePayment(PaymentRequest paymentRequest) {
        Payment payment = new Payment();
        payment.setOrderId(paymentRequest.getOrderId());
        payment.setAmount(paymentRequest.getAmount());
        payment.setCurrency(paymentRequest.getCurrency());
        payment.setPaymentMethod(paymentRequest.getPaymentMethod());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionId(generateTransactionId());
        payment.setCreatedAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    @Transactional
    public void completePayment(Long paymentId, PaymentResponse response, Throwable error, String eventId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();

        if (response != null && "SUCCESS".equals(response.getStatus())) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setCompletedAt(LocalDateTime.now());
            payment.setGatewayTransactionId(response.getGatewayTransactionId());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorMessage(error != null ? error.getMessage() : (response != null ? response.getErrorMessage() : "Unknown error"));
            if (response != null) {
                payment.setGatewayTransactionId(response.getGatewayTransactionId());
            }
        }
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        if (eventId != null) {
            processedEventRepository.save(new org.example.paymentService.model.ProcessedEvent(eventId));
        }

        try {
            org.example.common.event.PaymentResultEvent resultEvent = new org.example.common.event.PaymentResultEvent(
                    payment.getOrderId(), payment.getStatus().name(), payment.getTransactionId(), payment.getErrorMessage());
            String payload = objectMapper.writeValueAsString(resultEvent);
            org.example.paymentService.model.OutboxEvent outboxEvent = new org.example.paymentService.model.OutboxEvent(
                    org.example.paymentService.config.RabbitMQConfig.EXCHANGE_NAME,
                    org.example.paymentService.config.RabbitMQConfig.PAYMENT_RESULT_ROUTING_KEY,
                    payload);
            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize PaymentResultEvent", e);
        }
    }

    public PaymentResponse getPaymentStatus(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionId", transactionId));

        PaymentResponse response = new PaymentResponse();
        response.setTransactionId(payment.getTransactionId());
        response.setStatus(payment.getStatus().name());
        response.setGatewayTransactionId(payment.getGatewayTransactionId());
        response.setErrorMessage(payment.getErrorMessage());

        return response;
    }

    public PaymentResponse refundPayment(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionId", transactionId));

        if (!payment.getStatus().canTransitionTo(PaymentStatus.REFUNDED)) {
            throw new InvalidOperationException(
                    "Cannot refund payment in status: " + payment.getStatus() +
                            ". Only SUCCESS payments can be refunded.");
        }

        try {
            PaymentResponse refundResponse = paymentGatewayClient
                    .refundPayment(payment.getTransactionId(), payment.getGatewayTransactionId())
                    .block();

            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAt(LocalDateTime.now());
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            return refundResponse;
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.REFUND_FAILED);
            payment.setErrorMessage("Refund failed: " + e.getMessage());
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            PaymentResponse errorResponse = new PaymentResponse();
            errorResponse.setTransactionId(payment.getTransactionId());
            errorResponse.setStatus("REFUND_FAILED");
            errorResponse.setErrorMessage("Refund processing failed: " + e.getMessage());

            return errorResponse;
        }
    }

    private String generateTransactionId() {
        return UUID.randomUUID().toString();
    }
}