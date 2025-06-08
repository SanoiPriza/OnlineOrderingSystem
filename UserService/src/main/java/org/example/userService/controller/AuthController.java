package org.example.userService.controller;

import org.example.common.security.jwt.JwtTokenUtil;
import org.example.userService.dto.AuthRequest;
import org.example.userService.dto.AuthResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;

    public AuthController(
            JwtTokenUtil jwtTokenUtil,
            @Qualifier("userServiceUserDetailsService") UserDetailsService userDetailsService,
            AuthenticationManager authenticationManager) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(),
                            authRequest.getPassword()
                    )
            );

            UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getUsername());

            String token = jwtTokenUtil.generateToken(userDetails);

            AuthResponse response = new AuthResponse(
                    token,
                    userDetails.getUsername(),
                    userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList())
            );

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (DisabledException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Account is disabled");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Authentication failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<Boolean> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractTokenFromHeader(authHeader);
            if (token == null) {
                return ResponseEntity.ok(false);
            }

            String username = jwtTokenUtil.extractUsername(token);
            if (username != null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                boolean isValid = jwtTokenUtil.validateToken(token, userDetails);
                return ResponseEntity.ok(isValid);
            }
            return ResponseEntity.ok(false);
        } catch (Exception e) {
            return ResponseEntity.ok(false);
        }
    }

    @GetMapping("/username")
    public ResponseEntity<?> extractUsername(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractTokenFromHeader(authHeader);
            if (token == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid authorization header"));
            }

            String username = jwtTokenUtil.extractUsername(token);
            if (username != null) {
                return ResponseEntity.ok(Map.of("username", username));
            }
            return ResponseEntity.badRequest().body(createErrorResponse("Invalid token"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Token extraction failed"));
        }
    }

    @GetMapping("/user-details")
    public ResponseEntity<?> getUserDetails(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractTokenFromHeader(authHeader);
            if (token == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid authorization header"));
            }

            String username = jwtTokenUtil.extractUsername(token);
            if (username != null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtTokenUtil.validateToken(token, userDetails)) {
                    Map<String, Object> safeUserDetails = new HashMap<>();
                    safeUserDetails.put("username", userDetails.getUsername());
                    safeUserDetails.put("authorities", userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList()));
                    safeUserDetails.put("enabled", userDetails.isEnabled());
                    safeUserDetails.put("accountNonExpired", userDetails.isAccountNonExpired());
                    safeUserDetails.put("accountNonLocked", userDetails.isAccountNonLocked());
                    safeUserDetails.put("credentialsNonExpired", userDetails.isCredentialsNonExpired());

                    return ResponseEntity.ok(safeUserDetails);
                }
            }
            return ResponseEntity.badRequest().body(createErrorResponse("Invalid token"));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.badRequest().body(createErrorResponse("User not found"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to retrieve user details"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractTokenFromHeader(authHeader);
            if (token == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid authorization header"));
            }

            String username = jwtTokenUtil.extractUsername(token);
            if (username != null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtTokenUtil.validateToken(token, userDetails)) {
                    String newToken = jwtTokenUtil.generateToken(userDetails);

                    AuthResponse response = new AuthResponse(
                            newToken,
                            userDetails.getUsername(),
                            userDetails.getAuthorities().stream()
                                    .map(GrantedAuthority::getAuthority)
                                    .collect(Collectors.toList())
                    );

                    return ResponseEntity.ok(response);
                }
            }
            return ResponseEntity.badRequest().body(createErrorResponse("Invalid token"));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.badRequest().body(createErrorResponse("User not found"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Token refresh failed"));
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractTokenFromHeader(authHeader);
            if (token == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid authorization header"));
            }

            String username = jwtTokenUtil.extractUsername(token);
            if (username != null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                boolean isValid = jwtTokenUtil.validateToken(token, userDetails);

                Map<String, Object> response = new HashMap<>();
                response.put("valid", isValid);
                response.put("username", username);
                response.put("authorities", userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()));

                return ResponseEntity.ok(response);
            }
            return ResponseEntity.badRequest().body(createErrorResponse("Invalid token"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Token verification failed"));
        }
    }

    private String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}