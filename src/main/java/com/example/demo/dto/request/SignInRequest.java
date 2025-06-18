package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request payload for user authentication/signin.
 * Supports authentication using either username or email address.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.controller.AuthController#signIn
 * @see com.example.demo.service.interfaces.CognitoService#signIn
 * @see com.example.demo.dto.response.AuthResponse
 */
@Data
public class SignInRequest {

    /**
     * Login identifier - can be either username or email address.
     * The system will automatically detect and handle both formats.
     */
    @NotBlank(message = "Login identifier is required")
    private String login; // Can be username or email

    /**
     * User's password for authentication.
     * Must match the password set during registration or last password reset.
     */
    @NotBlank(message = "Password is required")
    private String password;
}
