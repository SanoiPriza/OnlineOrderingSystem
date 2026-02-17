package org.example.common.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.common.security.service.AuthValidationService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

public class DelegatedJwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthValidationService authValidationService;

    public DelegatedJwtAuthenticationFilter(AuthValidationService authValidationService) {
        this.authValidationService = authValidationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");

        String username = null;
        String jwtToken = null;

        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);

            if (jwtToken.trim().isEmpty()) {
                chain.doFilter(request, response);
                return;
            }

            try {
                if (authValidationService.validateToken(jwtToken)) {
                    username = authValidationService.extractUsername(jwtToken);
                }
            } catch (Exception e) {
                logger.error("Unable to validate JWT Token or extract username", e);
            }
        }

        if (username != null && !username.trim().isEmpty() && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = User.builder()
                    .username(username)
                    .password("")
                    .authorities(new ArrayList<>())
                    .build();

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        chain.doFilter(request, response);
    }
}