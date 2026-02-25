package org.example.paymentService.service;

import org.example.paymentService.model.PaymentRequest;
import org.example.paymentService.model.PaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentGatewayClient {

    private static final Duration GATEWAY_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;

    public PaymentGatewayClient(
            WebClient.Builder webClientBuilder,
            @Value("${payment.gateway.url}") String gatewayUrl,
            @Value("${payment.gateway.apiKey}") String apiKey) {
        this.webClient = webClientBuilder
                .baseUrl(gatewayUrl)
                .defaultHeader("X-API-Key", apiKey)
                .build();
    }

    public Mono<PaymentResponse> processPayment(String transactionId, PaymentRequest paymentRequest) {
        Map<String, Object> body = new HashMap<>();
        body.put("transactionId", transactionId);
        body.put("amount", paymentRequest.getAmount());
        body.put("currency", paymentRequest.getCurrency());
        body.put("paymentMethod", paymentRequest.getPaymentMethod());

        return webClient.post()
                .uri("/process")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(PaymentResponse.class)
                .timeout(GATEWAY_TIMEOUT);
    }

    public Mono<PaymentResponse> refundPayment(String transactionId, String gatewayTransactionId) {
        Map<String, Object> body = new HashMap<>();
        body.put("transactionId", transactionId);
        body.put("gatewayTransactionId", gatewayTransactionId);

        return webClient.post()
                .uri("/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(PaymentResponse.class)
                .timeout(GATEWAY_TIMEOUT);
    }
}
