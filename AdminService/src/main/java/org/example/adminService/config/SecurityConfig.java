package org.example.adminService.config;

import org.example.common.security.config.CommonWebSecurityConfig;
import org.example.common.security.jwt.JwtAuthenticationEntryPoint;
import org.example.common.security.jwt.JwtTokenUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig extends CommonWebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtTokenUtil jwtTokenUtil,
            JwtAuthenticationEntryPoint entryPoint,
            @org.springframework.beans.factory.annotation.Value("${internal.service.token}") String internalToken) throws Exception {
        return configureDelegatedSecurityFilterChain(
                http,
                createDelegatedJwtAuthenticationFilter(jwtTokenUtil),
                entryPoint,
                internalToken);
    }
}
