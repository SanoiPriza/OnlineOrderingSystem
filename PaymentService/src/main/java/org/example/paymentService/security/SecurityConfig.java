package org.example.paymentService.security;

import org.example.common.security.config.CommonWebSecurityConfig;
import org.example.common.security.jwt.DelegatedJwtAuthenticationFilter;
import org.example.common.security.jwt.JwtAuthenticationEntryPoint;
import org.example.common.security.service.AuthValidationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration("paymentSecurityConfig")
@EnableWebSecurity
public class SecurityConfig extends CommonWebSecurityConfig {

    private final AuthValidationService authValidationService;
    private final JwtAuthenticationEntryPoint jwtAuthEntryPoint;

    public SecurityConfig(AuthValidationService authValidationService,
                          JwtAuthenticationEntryPoint jwtAuthEntryPoint) {
        this.authValidationService = authValidationService;
        this.jwtAuthEntryPoint = jwtAuthEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        DelegatedJwtAuthenticationFilter delegatedAuthFilter =
                createDelegatedJwtAuthenticationFilter(authValidationService);

        return configureDelegatedSecurityFilterChain(http, delegatedAuthFilter, jwtAuthEntryPoint);
    }
}