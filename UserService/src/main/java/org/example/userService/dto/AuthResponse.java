package org.example.userService.dto;

import java.util.List;

public class AuthResponse {
    private String token;
    private String username;
    private List<String> authorities;

    public AuthResponse() {}

    public AuthResponse(String token, String username, List<String> authorities) {
        this.token = token;
        this.username = username;
        this.authorities = authorities;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
    }
}