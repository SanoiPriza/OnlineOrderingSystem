package org.example.common.security.config;

import org.example.common.security.jwt.DelegatedJwtAuthenticationFilter;
import org.example.common.security.jwt.JwtAuthenticationEntryPoint;
import org.example.common.security.jwt.JwtAuthenticationFilter;
import org.example.common.security.jwt.JwtTokenUtil;
import org.example.common.security.service.AuthValidationService;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public abstract class CommonWebSecurityConfig {

    protected SecurityFilterChain configureSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthFilter,
            JwtAuthenticationEntryPoint jwtAuthEntryPoint) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthEntryPoint)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    protected SecurityFilterChain configureDelegatedSecurityFilterChain(
            HttpSecurity http,
            DelegatedJwtAuthenticationFilter delegatedAuthFilter,
            JwtAuthenticationEntryPoint jwtAuthEntryPoint) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthEntryPoint)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(delegatedAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    protected JwtAuthenticationFilter createJwtAuthenticationFilter(
            JwtTokenUtil jwtTokenUtil,
            UserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtTokenUtil, userDetailsService);
    }

    protected DelegatedJwtAuthenticationFilter createDelegatedJwtAuthenticationFilter(
            AuthValidationService authValidationService) {
        return new DelegatedJwtAuthenticationFilter(authValidationService);
    }
}