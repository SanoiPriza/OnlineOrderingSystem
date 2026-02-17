package org.example.orderService.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.example.orderService.dto.PaymentRequest;
import org.example.orderService.dto.PaymentResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Component
public class PaymentServiceClient {

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;

    public PaymentServiceClient(
            WebClient.Builder webClientBuilder,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.webClient = webClientBuilder
                .baseUrl("lb://payment-service")
                .build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("paymentService");
    }

    public CompletableFuture<PaymentResponse> processPaymentCompletable(PaymentRequest paymentRequest) {
        return processPayment(paymentRequest).toFuture();
    }

    public Mono<PaymentResponse> processPayment(PaymentRequest paymentRequest) {
        return webClient.post()
                .uri("/api/payments/process")
                .bodyValue(paymentRequest)
                .retrieve()
                .bodyToMono(PaymentResponse.class)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(e -> {
                    PaymentResponse errorResponse = new PaymentResponse();
                    errorResponse.setStatus("FAILED");
                    errorResponse.setErrorMessage("Payment service unavailable: " + e.getMessage());
                    return Mono.just(errorResponse);
                });
    }

    public CompletableFuture<PaymentResponse> getPaymentStatusCompletable(String transactionId) {
        return getPaymentStatus(transactionId).toFuture();
    }

    public Mono<PaymentResponse> getPaymentStatus(String transactionId) {
        return webClient.get()
                .uri("/api/payments/{transactionId}", transactionId)
                .retrieve()
                .bodyToMono(PaymentResponse.class)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(e -> {
                    PaymentResponse errorResponse = new PaymentResponse();
                    errorResponse.setStatus("UNKNOWN");
                    errorResponse.setErrorMessage("Payment status service unavailable: " + e.getMessage());
                    return Mono.just(errorResponse);
                });
    }

    public CompletableFuture<PaymentResponse> refundPaymentCompletable(String transactionId) {
        return refundPayment(transactionId).toFuture();
    }

    public Mono<PaymentResponse> refundPayment(String transactionId) {
        return webClient.post()
                .uri("/api/payments/{transactionId}/refund", transactionId)
                .retrieve()
                .bodyToMono(PaymentResponse.class)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(e -> {
                    PaymentResponse errorResponse = new PaymentResponse();
                    errorResponse.setStatus("REFUND_FAILED");
                    errorResponse.setErrorMessage("Refund service unavailable: " + e.getMessage());
                    return Mono.just(errorResponse);
                });
    }
}