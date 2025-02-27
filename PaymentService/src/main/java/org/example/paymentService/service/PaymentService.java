package org.example.paymentService.service;

import org.example.paymentService.model.Payment;
import org.example.paymentService.model.PaymentRequest;
import org.example.paymentService.model.PaymentResponse;
import org.example.paymentService.repository.PaymentRepository;
import org.springframework.stereotype.Service;

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

    public PaymentResponse processPayment(PaymentRequest paymentRequest) {
        Payment payment = new Payment();
        payment.setOrderId(paymentRequest.getOrderId());
        payment.setAmount(paymentRequest.getAmount());
        payment.setCurrency(paymentRequest.getCurrency());
        payment.setPaymentMethod(paymentRequest.getPaymentMethod());
        payment.setStatus("PENDING");
        payment.setTransactionId(generateTransactionId());
        payment.setCreatedAt(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);
        
        try {
            PaymentResponse gatewayResponse = paymentGatewayClient.processPayment(
                savedPayment.getTransactionId(),
                paymentRequest
            );

            savedPayment.setStatus(gatewayResponse.getStatus());
            savedPayment.setGatewayTransactionId(gatewayResponse.getGatewayTransactionId());
            savedPayment.setUpdatedAt(LocalDateTime.now());
            
            if ("SUCCESS".equals(gatewayResponse.getStatus())) {
                savedPayment.setCompletedAt(LocalDateTime.now());
            }

            paymentRepository.save(savedPayment);
            
            return gatewayResponse;
        } catch (Exception e) {
            savedPayment.setStatus("FAILED");
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
            .orElseThrow(() -> new RuntimeException("Payment not found with transaction ID: " + transactionId));
        
        PaymentResponse response = new PaymentResponse();
        response.setTransactionId(payment.getTransactionId());
        response.setStatus(payment.getStatus());
        response.setGatewayTransactionId(payment.getGatewayTransactionId());
        response.setErrorMessage(payment.getErrorMessage());
        
        return response;
    }
    
    public PaymentResponse refundPayment(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new RuntimeException("Payment not found with transaction ID: " + transactionId));
        
        if (!"SUCCESS".equals(payment.getStatus())) {
            throw new RuntimeException("Cannot refund payment that is not successful");
        }
        
        try {
            PaymentResponse refundResponse = paymentGatewayClient.refundPayment(
                payment.getTransactionId(),
                payment.getGatewayTransactionId()
            );

            payment.setStatus("REFUNDED");
            payment.setRefundedAt(LocalDateTime.now());
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            
            return refundResponse;
        } catch (Exception e) {
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