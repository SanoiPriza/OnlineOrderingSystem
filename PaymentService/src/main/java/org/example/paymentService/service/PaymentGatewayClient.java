package org.example.paymentService.service;

import org.example.paymentService.model.PaymentRequest;
import org.example.paymentService.model.PaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentGatewayClient {

    private final WebClient webClient;
    private final String apiKey;

    public PaymentGatewayClient(
            WebClient.Builder webClientBuilder,
            @Value("${payment.gateway.url}") String gatewayUrl,
            @Value("${payment.gateway.apiKey}") String apiKey) {
        this.webClient = webClientBuilder
                .baseUrl(gatewayUrl)
                .defaultHeader("X-API-Key", apiKey)
                .build();
        this.apiKey = apiKey;
    }

    public PaymentResponse processPayment(String transactionId, PaymentRequest paymentRequest) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("transactionId", transactionId);
        requestMap.put("amount", paymentRequest.getAmount());
        requestMap.put("currency", paymentRequest.getCurrency());
        requestMap.put("paymentMethod", paymentRequest.getPaymentMethod());
        requestMap.put("cardDetails", createCardDetailsMap(paymentRequest));

        return webClient.post()
                .uri("/process")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestMap)
                .retrieve()
                .bodyToMono(PaymentResponse.class)
                .block();
    }

    public PaymentResponse refundPayment(String transactionId, String gatewayTransactionId) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("transactionId", transactionId);
        requestMap.put("gatewayTransactionId", gatewayTransactionId);

        return webClient.post()
                .uri("/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestMap)
                .retrieve()
                .bodyToMono(PaymentResponse.class)
                .block();
    }

    private Map<String, String> createCardDetailsMap(PaymentRequest paymentRequest) {
        Map<String, String> cardDetails = new HashMap<>();
        cardDetails.put("number", paymentRequest.getCardNumber());
        cardDetails.put("expiryMonth", paymentRequest.getCardExpiryMonth());
        cardDetails.put("expiryYear", paymentRequest.getCardExpiryYear());
        cardDetails.put("cvv", paymentRequest.getCardCvv());
        cardDetails.put("holderName", paymentRequest.getCardHolderName());
        return cardDetails;
    }
}