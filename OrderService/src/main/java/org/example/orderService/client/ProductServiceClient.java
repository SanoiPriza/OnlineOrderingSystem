package org.example.orderService.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.example.orderService.dto.ProductResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class ProductServiceClient {

    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;

    public ProductServiceClient(
            WebClient.Builder webClientBuilder,
            CircuitBreakerRegistry circuitBreakerRegistry,
            InternalTokenProvider internalTokenProvider) {
        this.webClient = webClientBuilder
                .baseUrl("lb://product-service")
                .filter((request, next) -> {
                    org.springframework.web.reactive.function.client.ClientRequest newRequest = org.springframework.web.reactive.function.client.ClientRequest
                            .from(request)
                            .header(org.springframework.http.HttpHeaders.AUTHORIZATION,
                                    "Bearer " + internalTokenProvider.getToken())
                            .build();
                    return next.exchange(newRequest);
                })
                .build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("productService");
    }

    public ProductResponse getProductById(String productId) {
        return webClient.get()
                .uri("/api/products/{id}", productId)
                .retrieve()
                .bodyToMono(ProductResponse.class)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .timeout(CALL_TIMEOUT)
                .block();
    }
}
