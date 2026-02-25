package org.example.userService.controller;

import org.example.common.security.jwt.JwtTokenUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ClientTokenController {

    private static final Logger log = LoggerFactory.getLogger(ClientTokenController.class);
    private final JwtTokenUtil jwtTokenUtil;

    private static final String CLIENT_ID = "internal-service";
    private static final String CLIENT_SECRET = "secret";

    public ClientTokenController(JwtTokenUtil jwtTokenUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @PostMapping("/client-token")
    public ResponseEntity<?> getClientToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid Authorization header");
        }

        try {
            String base64Credentials = authHeader.substring(6).trim();
            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(credDecoded, StandardCharsets.UTF_8);
            final String[] values = credentials.split(":", 2);

            if (values.length != 2 || !CLIENT_ID.equals(values[0]) || !CLIENT_SECRET.equals(values[1])) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid client credentials");
            }

            org.springframework.security.core.userdetails.User dummyUser = new org.springframework.security.core.userdetails.User(
                    CLIENT_ID, "", java.util.List.of(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                    "ROLE_INTERNAL_SERVICE"),
                            new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));
            String token = jwtTokenUtil.generateToken(dummyUser, dummyUser.getAuthorities());
            log.info("Client credentials auth successful, generated JWT for internal service.");

            return ResponseEntity.ok(Map.of("access_token", token, "token_type", "Bearer", "expires_in", 3600));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Authorization header format");
        }
    }
}
