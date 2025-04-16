package org.example.paymentService.client;

import org.example.paymentService.model.AuthRequest;
import org.example.paymentService.model.AuthResponse;
import org.example.paymentService.model.UserDetailsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @PostMapping("/api/auth/login")
    AuthResponse login(@RequestBody AuthRequest authRequest);

    @GetMapping("/api/users/username/{username}")
    UserDetailsResponse getUserByUsername(@PathVariable("username") String username);
    
    @GetMapping("/api/users/validate-token")
    boolean validateToken(@RequestBody String token);
}