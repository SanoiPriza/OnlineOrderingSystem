package org.example.orderService.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.example.common.exception.InvalidOperationException;
import org.example.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Component
public class ProductServiceClient {

    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;

    public ProductServiceClient(
            WebClient.Builder webClientBuilder,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${internal.service.token}") String serviceToken) {
        this.webClient = webClientBuilder
                .baseUrl("lb://product-service")
                .defaultHeader("X-Internal-Service-Token", serviceToken)
                .build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("productService");
    }

    public void decrementStockBlocking(String productId, int quantity) {
        webClient.put()
                .uri("/api/products/{id}/stock/decrement?quantity={qty}", productId, quantity)
                .retrieve()
                .onStatus(
                        status -> status == HttpStatus.UNPROCESSABLE_ENTITY,
                        response -> response.bodyToMono(String.class)
                                .map(body -> new InvalidOperationException(
                                        "Insufficient stock for product "
                                                + productId + ": "
                                                + body)))
                .onStatus(
                        status -> status == HttpStatus.NOT_FOUND,
                        response -> Mono.error(new ResourceNotFoundException("Product", "id",
                                productId)))
                .bodyToMono(Void.class)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .timeout(CALL_TIMEOUT)
                .block();
    }

    public CompletableFuture<Void> decrementStock(String productId, int quantity) {
        return webClient.put()
                .uri("/api/products/{id}/stock/decrement?quantity={qty}", productId, quantity)
                .retrieve()
                .onStatus(
                        status -> status == HttpStatus.UNPROCESSABLE_ENTITY,
                        response -> response.bodyToMono(String.class)
                                .map(body -> new InvalidOperationException(
                                        "Insufficient stock for product "
                                                + productId + ": "
                                                + body)))
                .onStatus(
                        status -> status == HttpStatus.NOT_FOUND,
                        response -> Mono.error(new ResourceNotFoundException("Product", "id",
                                productId)))
                .bodyToMono(Void.class)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .timeout(CALL_TIMEOUT)
                .toFuture();
    }

    public CompletableFuture<Void> incrementStock(String productId, int quantity) {
        return webClient.put()
                .uri("/api/products/{id}/stock/increment?quantity={qty}", productId, quantity)
                .retrieve()
                .bodyToMono(Void.class)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .timeout(CALL_TIMEOUT)
                .toFuture();
    }
}
