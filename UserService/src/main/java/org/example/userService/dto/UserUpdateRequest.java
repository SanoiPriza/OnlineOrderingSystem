package org.example.userService.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UserUpdateRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    public UserUpdateRequest() {
    }

    public UserUpdateRequest(String email, String firstName, String lastName) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
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

    public static UserUpdateRequestBuilder builder() {
        return new UserUpdateRequestBuilder();
    }

    public static class UserUpdateRequestBuilder {
        private String email;
        private String firstName;
        private String lastName;

        public UserUpdateRequestBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserUpdateRequestBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public UserUpdateRequestBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public UserUpdateRequest build() {
            return new UserUpdateRequest(email, firstName, lastName);
        }
    }
}
