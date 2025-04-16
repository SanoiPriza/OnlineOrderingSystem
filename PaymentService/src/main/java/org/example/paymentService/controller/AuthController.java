package org.example.paymentService.controller;

import org.example.paymentService.client.UserServiceClient;
import org.example.paymentService.model.AuthRequest;
import org.example.paymentService.model.AuthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}