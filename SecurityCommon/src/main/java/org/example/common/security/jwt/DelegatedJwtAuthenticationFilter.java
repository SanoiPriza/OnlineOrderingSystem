package org.example.common.security.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class DelegatedJwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;

    public DelegatedJwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");

        if (requestTokenHeader == null || !requestTokenHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String jwtToken = requestTokenHeader.substring(7);
        if (jwtToken.trim().isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String username = jwtTokenUtil.extractUsername(jwtToken);
            if (username == null || username.trim().isEmpty()) {
                chain.doFilter(request, response);
                return;
            }

            List<SimpleGrantedAuthority> authorities = extractAuthorities(jwtToken);

            UserDetails userDetails = User.builder()
                    .username(username)
                    .password("")
                    .authorities(authorities)
                    .build();

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

        } catch (Exception e) {
            logger.warn("JWT validation failed in DelegatedJwtAuthenticationFilter: " + e.getMessage());
        }

        chain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> extractAuthorities(String token) {
        try {
            Object rolesObj = jwtTokenUtil.extractClaim(token, claims -> claims.get("roles"));
            if (rolesObj instanceof List<?> rawList) {
                return rawList.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .map(SimpleGrantedAuthority::new)
                        .toList();
            }
        } catch (Exception ignored) {}
        return Collections.emptyList();
    }
}
