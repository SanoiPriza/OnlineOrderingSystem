package org.example.paymentService.service;

import org.example.paymentService.model.PaymentRequest;
import org.example.paymentService.model.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayClient.class);
    private static final Duration GATEWAY_TIMEOUT = Duration.ofSeconds(10);

    private final boolean simulateMode;
    private final WebClient webClient;

    public PaymentGatewayClient(
            WebClient.Builder webClientBuilder,
            @Value("${payment.gateway.url}") String gatewayUrl,
            @Value("${payment.gateway.apiKey}") String apiKey,
            @Value("${payment.gateway.simulate:true}") boolean simulateMode) {

        this.simulateMode = simulateMode;
        this.webClient = webClientBuilder
                .baseUrl(gatewayUrl)
                .defaultHeader("X-API-Key", apiKey)
                .build();

        if (simulateMode) {
            log.warn("PaymentGatewayClient is running in SIMULATION MODE. "
                    + "No real HTTP calls will be made to the payment gateway. "
                    + "Set payment.gateway.simulate=false to use a real gateway.");
        }
    }

    public Mono<PaymentResponse> processPayment(String transactionId, PaymentRequest paymentRequest) {
        if (simulateMode) {
            return simulateProcessPayment(transactionId, paymentRequest);
        }
        return callGatewayProcessPayment(transactionId, paymentRequest);
    }

    public Mono<PaymentResponse> refundPayment(String transactionId, String gatewayTransactionId) {
        if (simulateMode) {
            return simulateRefundPayment(transactionId, gatewayTransactionId);
        }
        return callGatewayRefundPayment(transactionId, gatewayTransactionId);
    }

    private Mono<PaymentResponse> simulateProcessPayment(String transactionId,
                                                          PaymentRequest paymentRequest) {
        boolean willSucceed = paymentRequest.getAmount() == null
                || paymentRequest.getAmount().compareTo(new BigDecimal("10000")) < 0;

        String gatewayTxId = "SIM-" + UUID.randomUUID();
        log.info("[SIM] Processing payment txId={} amount={} → {}",
                transactionId, paymentRequest.getAmount(), willSucceed ? "SUCCESS" : "FAILED");

        PaymentResponse response = new PaymentResponse();
        response.setTransactionId(transactionId);
        response.setGatewayTransactionId(gatewayTxId);

        if (willSucceed) {
            response.setStatus("SUCCESS");
        } else {
            response.setStatus("FAILED");
            response.setErrorMessage("[SIM] Amount >= 10000 triggers simulated failure");
        }

        return Mono.just(response);
    }

    private Mono<PaymentResponse> simulateRefundPayment(String transactionId,
                                                         String gatewayTransactionId) {
        log.info("[SIM] Refunding payment txId={} gatewayTxId={}", transactionId, gatewayTransactionId);

        PaymentResponse response = new PaymentResponse();
        response.setTransactionId(transactionId);
        response.setGatewayTransactionId(gatewayTransactionId);
        response.setStatus("REFUNDED");

        return Mono.just(response);
    }

    private Mono<PaymentResponse> callGatewayProcessPayment(String transactionId,
                                                              PaymentRequest paymentRequest) {
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

    private Mono<PaymentResponse> callGatewayRefundPayment(String transactionId,
                                                             String gatewayTransactionId) {
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
