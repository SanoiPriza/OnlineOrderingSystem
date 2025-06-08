package org.example.paymentService.client;

import org.example.paymentService.model.AuthRequest;
import org.example.paymentService.model.AuthResponse;
import org.example.paymentService.model.UserDetailsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @PostMapping("/api/auth/login")
    AuthResponse login(@RequestBody AuthRequest authRequest);

    @GetMapping("/api/users/username/{username}")
    UserDetailsResponse getUserByUsername(@PathVariable("username") String username);

    @GetMapping("/api/auth/validate")
    boolean validateToken(@RequestHeader("Authorization") String authHeader);

    @GetMapping("/api/auth/username")
    String extractUsername(@RequestHeader("Authorization") String authHeader);

    @GetMapping("/api/auth/user-details")
    UserDetailsResponse getUserDetails(@RequestHeader("Authorization") String authHeader);

    @PostMapping("/api/auth/refresh")
    AuthResponse refreshToken(@RequestHeader("Authorization") String authHeader);

    @GetMapping("/api/auth/verify")
    boolean verifyToken(@RequestHeader("Authorization") String authHeader);
}