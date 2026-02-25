package org.example.paymentService.config;

import org.example.common.security.config.CommonWebSecurityConfig;
import org.example.common.security.jwt.JwtAuthenticationEntryPoint;
import org.example.common.security.jwt.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class PaymentServiceSecurityConfig extends CommonWebSecurityConfig {

    @Value("${internal.service.token}")
    private String internalServiceToken;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtTokenUtil jwtTokenUtil,
            JwtAuthenticationEntryPoint entryPoint) throws Exception {
        return configureDelegatedSecurityFilterChain(
                http,
                createDelegatedJwtAuthenticationFilter(jwtTokenUtil),
                entryPoint,
                internalServiceToken);
    }
}
