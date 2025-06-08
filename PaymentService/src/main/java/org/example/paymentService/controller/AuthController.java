package org.example.paymentService.controller;

import org.example.paymentService.client.UserServiceClient;
import org.example.paymentService.model.AuthRequest;
import org.example.paymentService.model.AuthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserServiceClient userServiceClient;

    public AuthController(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest) {
        AuthResponse response = userServiceClient.login(authRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    public ResponseEntity<Boolean> validateToken(@RequestHeader("Authorization") String authHeader) {
        boolean isValid = userServiceClient.validateToken(authHeader);
        return ResponseEntity.ok(isValid);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {
        AuthResponse response = userServiceClient.refreshToken(authHeader);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify")
    public ResponseEntity<Boolean> verifyToken(@RequestHeader("Authorization") String authHeader) {
        boolean isValid = userServiceClient.verifyToken(authHeader);
        return ResponseEntity.ok(isValid);
    }
}