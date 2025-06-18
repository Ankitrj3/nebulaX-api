package com.example.demo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request payload for confirming user account signup.
 * Used to verify user's email address using the confirmation code sent during
 * registration.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.controller.AuthController#confirmSignUp
 * @see com.example.demo.service.interfaces.CognitoService#confirmSignUp
 */
@Data
public class ConfirmSignUpRequest {

    /**
     * Username of the user confirming their account.
     * Must match the username used during signup.
     */
    private String username;

    /**
     * Email address of the user confirming their account.
     * Must match the email used during signup and be a valid email format.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    /**
     * Verification code sent to user's email during signup.
     * This code is typically 6 digits and expires after a certain time period.
     */
    @NotBlank(message = "Confirmation code is required")
    private String confirmationCode;
}
