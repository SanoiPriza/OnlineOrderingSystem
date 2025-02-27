package org.example.paymentService.service;

import org.example.paymentService.model.PaymentRequest;
import org.example.paymentService.model.PaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentGatewayClient {

    private final RestTemplate restTemplate;

    @Value("${payment.gateway.url}")
    private String gatewayUrl;

    @Value("${payment.gateway.apiKey}")
    private String apiKey;

    public PaymentGatewayClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public PaymentResponse processPayment(String transactionId, PaymentRequest paymentRequest) {
        HttpHeaders headers = createHeaders();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("transactionId", transactionId);
        requestMap.put("amount", paymentRequest.getAmount());
        requestMap.put("currency", paymentRequest.getCurrency());
        requestMap.put("paymentMethod", paymentRequest.getPaymentMethod());
        requestMap.put("cardDetails", createCardDetailsMap(paymentRequest));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestMap, headers);

        return restTemplate.postForObject(gatewayUrl + "/process", request, PaymentResponse.class);
    }

    public PaymentResponse refundPayment(String transactionId, String gatewayTransactionId) {
        HttpHeaders headers = createHeaders();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("transactionId", transactionId);
        requestMap.put("gatewayTransactionId", gatewayTransactionId);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestMap, headers);

        return restTemplate.postForObject(gatewayUrl + "/refund", request, PaymentResponse.class);
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", apiKey);
        return headers;
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