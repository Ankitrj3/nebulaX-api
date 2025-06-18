package com.example.demo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request payload for initiating password reset process.
 * Triggers sending of password reset code to user's email.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.controller.AuthController#forgotPassword
 * @see com.example.demo.service.interfaces.CognitoService#forgotPassword
 */
@Data
public class ForgotPasswordRequest {

    /**
     * Email address of the user requesting password reset.
     * Must be a valid email format and match an existing user account.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
}
