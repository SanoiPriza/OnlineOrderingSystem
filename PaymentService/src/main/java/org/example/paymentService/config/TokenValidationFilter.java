package org.example.paymentService.config;

import org.example.common.security.service.AuthValidationService;
import org.example.paymentService.security.PaymentServiceUserDetailsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TokenValidationFilter extends OncePerRequestFilter {

    private final AuthValidationService authValidationService;
    private final PaymentServiceUserDetailsService userDetailsService;

    public TokenValidationFilter(AuthValidationService authValidationService, 
                               PaymentServiceUserDetailsService userDetailsService) {
        this.authValidationService = authValidationService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (authValidationService.validateToken(token)) {
                String username = authValidationService.extractUsername(token);
                
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    try {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        
                        UsernamePasswordAuthenticationToken authToken = 
                            new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    } catch (Exception e) {
                        logger.error("Failed to set authentication", e);
                    }
                }
            }
        }
        
        filterChain.doFilter(request, response);
    }
}