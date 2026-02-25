package org.example.userService.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.common.security.jwt.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class BlacklistCheckFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BlacklistCheckFilter.class);

    private final JwtTokenUtil jwtTokenUtil;
    private final TokenBlacklist tokenBlacklist;

    public BlacklistCheckFilter(JwtTokenUtil jwtTokenUtil, TokenBlacklist tokenBlacklist) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.tokenBlacklist = tokenBlacklist;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            String jti = jwtTokenUtil.extractJti(token);
            if (jti != null && tokenBlacklist.isBlacklisted(jti)) {
                log.debug("Rejected blacklisted token JTI={}", jti);
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"error\":\"Token has been revoked. Please log in again.\"}");
                return;
            }
        } catch (Exception e) {
            log.debug("BlacklistCheckFilter could not parse token: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }
}
