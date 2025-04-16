package org.example.paymentService.security;

import org.example.paymentService.client.UserServiceClient;
import org.example.paymentService.model.UserDetailsResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Primary
public class PaymentServiceUserDetailsService implements UserDetailsService {

    private final UserServiceClient userServiceClient;

    public PaymentServiceUserDetailsService(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            UserDetailsResponse userResponse = userServiceClient.getUserByUsername(username);
            
            if (userResponse == null) {
                throw new UsernameNotFoundException("User not found with username: " + username);
            }
            
            List<SimpleGrantedAuthority> authorities = userResponse.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
            
            return new org.springframework.security.core.userdetails.User(
                    userResponse.getUsername(),
                    "",
                    userResponse.isActive(),
                    true,
                    true,
                    true,
                    authorities
            );
        } catch (Exception e) {
            throw new UsernameNotFoundException("Error fetching user: " + e.getMessage(), e);
        }
    }
}