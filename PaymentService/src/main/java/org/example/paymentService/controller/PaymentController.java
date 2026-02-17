package org.example.paymentService.controller;

import jakarta.validation.Valid;
import org.example.paymentService.model.PaymentRequest;
import org.example.paymentService.model.PaymentResponse;
import org.example.paymentService.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest paymentRequest) {
        PaymentResponse response = paymentService.processPayment(paymentRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<PaymentResponse> getPaymentStatus(@PathVariable String transactionId) {
        PaymentResponse response = paymentService.getPaymentStatus(transactionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{transactionId}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable String transactionId) {
        PaymentResponse response = paymentService.refundPayment(transactionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public String getStatus() {
        return "Payment Service is running!";
    }
}