package org.example.paymentService.config;

import org.example.common.security.service.AuthValidationService;
import org.example.paymentService.security.PaymentServiceUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthValidationService authValidationService;
    private final PaymentServiceUserDetailsService userDetailsService;

    public SecurityConfig(AuthValidationService authValidationService, 
                         PaymentServiceUserDetailsService userDetailsService) {
        this.authValidationService = authValidationService;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/payments/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(new TokenValidationFilter(authValidationService, userDetailsService), 
                           UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}