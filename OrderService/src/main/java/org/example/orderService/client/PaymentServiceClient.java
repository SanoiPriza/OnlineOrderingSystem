package org.example.orderService.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.example.orderService.dto.PaymentRequest;
import org.example.orderService.dto.PaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Component
public class PaymentServiceClient {

    private final WebClient.Builder webClientBuilder;
    private final CircuitBreaker circuitBreaker;
    private final String paymentServiceUrl;

    public PaymentServiceClient(
            WebClient.Builder webClientBuilder,
            @Value("${payment.service.url:http://localhost:8083}") String paymentServiceUrl) {
        this.webClientBuilder = webClientBuilder;
        this.paymentServiceUrl = paymentServiceUrl;

        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .slidingWindowSize(10)
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("paymentService");
    }

    public CompletableFuture<PaymentResponse> processPaymentCompletable(PaymentRequest paymentRequest) {
        return processPayment(paymentRequest)
                .toFuture();
    }

    public Mono<PaymentResponse> processPayment(PaymentRequest paymentRequest) {
        return webClientBuilder.build()
                .post()
                .uri(paymentServiceUrl + "/api/payments/process")
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
        return getPaymentStatus(transactionId)
                .toFuture();
    }

    public Mono<PaymentResponse> getPaymentStatus(String transactionId) {
        return webClientBuilder.build()
                .get()
                .uri(paymentServiceUrl + "/api/payments/" + transactionId)
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
        return refundPayment(transactionId)
                .toFuture();
    }

    public Mono<PaymentResponse> refundPayment(String transactionId) {
        return webClientBuilder.build()
                .post()
                .uri(paymentServiceUrl + "/api/payments/" + transactionId + "/refund")
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