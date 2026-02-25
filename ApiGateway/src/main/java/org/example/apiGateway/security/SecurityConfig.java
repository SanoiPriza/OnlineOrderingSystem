package org.example.apiGateway.security;

import org.example.common.security.jwt.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public JwtTokenUtil jwtTokenUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {
        return new JwtTokenUtil(secret, expiration);
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http, JwtTokenUtil jwtTokenUtil) {

        AuthenticationWebFilter authWebFilter =
                new AuthenticationWebFilter(new JwtReactiveAuthenticationManager(jwtTokenUtil));
        authWebFilter.setServerAuthenticationConverter(new BearerTokenServerAuthenticationConverter());

        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/api/auth/**").permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterAt(authWebFilter, SecurityWebFiltersOrder.AUTHENTICATION);

        return http.build();
    }
}
