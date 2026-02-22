package org.example.userService.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserCreateRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100)
    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    public UserCreateRequest() {
    }

    public UserCreateRequest(String username, String password, String email, String firstName, String lastName) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public static UserCreateRequestBuilder builder() {
        return new UserCreateRequestBuilder();
    }

    public static class UserCreateRequestBuilder {
        private String username;
        private String password;
        private String email;
        private String firstName;
        private String lastName;

        public UserCreateRequestBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserCreateRequestBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UserCreateRequestBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserCreateRequestBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public UserCreateRequestBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public UserCreateRequest build() {
            return new UserCreateRequest(username, password, email, firstName, lastName);
        }
    }
}
