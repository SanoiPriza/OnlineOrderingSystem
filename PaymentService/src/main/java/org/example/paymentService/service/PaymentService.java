package org.example.paymentService.service;

import org.example.common.exception.InvalidOperationException;
import org.example.common.exception.ResourceNotFoundException;
import org.example.common.model.PaymentStatus;
import org.example.paymentService.model.Payment;
import org.example.paymentService.model.PaymentRequest;
import org.example.paymentService.model.PaymentResponse;
import org.example.paymentService.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayClient paymentGatewayClient;

    public PaymentService(PaymentRepository paymentRepository, PaymentGatewayClient paymentGatewayClient) {
        this.paymentRepository = paymentRepository;
        this.paymentGatewayClient = paymentGatewayClient;
    }

    @Transactional
    public PaymentResponse processPayment(PaymentRequest paymentRequest) {
        Payment payment = new Payment();
        payment.setOrderId(paymentRequest.getOrderId());
        payment.setAmount(paymentRequest.getAmount());
        payment.setCurrency(paymentRequest.getCurrency());
        payment.setPaymentMethod(paymentRequest.getPaymentMethod());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionId(generateTransactionId());
        payment.setCreatedAt(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);

        try {
            PaymentResponse gatewayResponse = paymentGatewayClient.processPayment(
                    savedPayment.getTransactionId(),
                    paymentRequest);

            String gatewayStatus = gatewayResponse.getStatus();
            if ("SUCCESS".equals(gatewayStatus)) {
                savedPayment.setStatus(PaymentStatus.SUCCESS);
                savedPayment.setCompletedAt(LocalDateTime.now());
            } else {
                savedPayment.setStatus(PaymentStatus.FAILED);
            }

            savedPayment.setGatewayTransactionId(gatewayResponse.getGatewayTransactionId());
            savedPayment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(savedPayment);

            return gatewayResponse;
        } catch (Exception e) {
            savedPayment.setStatus(PaymentStatus.FAILED);
            savedPayment.setErrorMessage(e.getMessage());
            savedPayment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(savedPayment);

            PaymentResponse errorResponse = new PaymentResponse();
            errorResponse.setTransactionId(savedPayment.getTransactionId());
            errorResponse.setStatus("FAILED");
            errorResponse.setErrorMessage("Payment processing failed: " + e.getMessage());

            return errorResponse;
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

    @Transactional
    public PaymentResponse refundPayment(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionId", transactionId));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new InvalidOperationException(
                    "Cannot refund payment that is not successful. Current status: " + payment.getStatus());
        }

        try {
            PaymentResponse refundResponse = paymentGatewayClient.refundPayment(
                    payment.getTransactionId(),
                    payment.getGatewayTransactionId());

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