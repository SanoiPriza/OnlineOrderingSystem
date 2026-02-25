package org.example.userService.security;

import org.example.common.security.config.CommonWebSecurityConfig;
import org.example.common.security.jwt.JwtAuthenticationEntryPoint;
import org.example.common.security.jwt.JwtAuthenticationFilter;
import org.example.common.security.jwt.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig extends CommonWebSecurityConfig {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthEntryPoint;
    private final TokenBlacklist tokenBlacklist;

    public SecurityConfig(
            JwtTokenUtil jwtTokenUtil,
            @Qualifier("userServiceUserDetailsService") UserDetailsService userDetailsService,
            JwtAuthenticationEntryPoint jwtAuthEntryPoint,
            TokenBlacklist tokenBlacklist) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
        this.jwtAuthEntryPoint = jwtAuthEntryPoint;
        this.tokenBlacklist = tokenBlacklist;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtAuthFilter = createJwtAuthenticationFilter(jwtTokenUtil, userDetailsService);
        BlacklistCheckFilter blacklistFilter = new BlacklistCheckFilter(jwtTokenUtil, tokenBlacklist);

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
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(blacklistFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
