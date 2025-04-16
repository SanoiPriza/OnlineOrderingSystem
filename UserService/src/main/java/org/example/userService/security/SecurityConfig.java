package org.example.userService.security;

import org.example.common.security.config.CommonWebSecurityConfig;
import org.example.common.security.jwt.JwtAuthenticationEntryPoint;
import org.example.common.security.jwt.JwtAuthenticationFilter;
import org.example.common.security.jwt.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig extends CommonWebSecurityConfig {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthEntryPoint;

    public SecurityConfig(
            JwtTokenUtil jwtTokenUtil, 
            @Qualifier("userServiceUserDetailsService") UserDetailsService userDetailsService, 
            JwtAuthenticationEntryPoint jwtAuthEntryPoint) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
        this.jwtAuthEntryPoint = jwtAuthEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtAuthFilter = createJwtAuthenticationFilter(jwtTokenUtil, userDetailsService);
        return configureSecurityFilterChain(http, jwtAuthFilter, jwtAuthEntryPoint);
    }
}