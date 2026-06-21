package org.example.apiGateway.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;

@Configuration
public class GatewayConfig {

    @Bean
    @Primary
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(30, 60, 1);
    }

    @Bean
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(principal -> "user:" + principal.getName())
                .switchIfEmpty(
                        Mono.justOrEmpty(exchange.getRequest().getRemoteAddress())
                                .map(InetSocketAddress::getAddress)
                                .map(InetAddress::getHostAddress)
                                .map(ip -> "ip:" + ip)
                                .defaultIfEmpty("ip:unknown"));
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder,
                               RedisRateLimiter redisRateLimiter,
                               @Qualifier("userOrIpKeyResolver") KeyResolver keyResolver) {

        return builder.routes()

                .route("user-service", r -> r
                        .path("/api/users/**", "/api/auth/**")
                        .filters(f -> f.requestRateLimiter(c -> {
                            c.setRateLimiter(redisRateLimiter);
                            c.setKeyResolver(keyResolver);
                            c.setDenyEmptyKey(false);
                        }))
                        .uri("lb://user-service"))

                .route("payment-service", r -> r
                        .path("/api/payments/**")
                        .filters(f -> f.requestRateLimiter(c -> {
                            c.setRateLimiter(redisRateLimiter);
                            c.setKeyResolver(keyResolver);
                            c.setDenyEmptyKey(false);
                        }))
                        .uri("lb://payment-service"))

                .route("order-service", r -> r
                        .path("/api/orders/**")
                        .filters(f -> f.requestRateLimiter(c -> {
                            c.setRateLimiter(redisRateLimiter);
                            c.setKeyResolver(keyResolver);
                            c.setDenyEmptyKey(false);
                        }))
                        .uri("lb://order-service"))

                .route("product-service", r -> r
                        .path("/api/products/**")
                        .filters(f -> f.requestRateLimiter(c -> {
                            c.setRateLimiter(redisRateLimiter);
                            c.setKeyResolver(keyResolver);
                            c.setDenyEmptyKey(false);
                        }))
                        .uri("lb://product-service"))

                // Admin internal routes for HealthCheck and Stats
                .route("admin-user-service", r -> r
                        .path("/user-service/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://user-service"))
                .route("admin-payment-service", r -> r
                        .path("/payment-service/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://payment-service"))
                .route("admin-order-service", r -> r
                        .path("/order-service/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://order-service"))
                .route("admin-product-service", r -> r
                        .path("/product-service/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://product-service"))
                .route("admin-discovery-service", r -> r
                        .path("/discovery-service/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("http://discovery-service:8761"))

                .build();
    }
}
