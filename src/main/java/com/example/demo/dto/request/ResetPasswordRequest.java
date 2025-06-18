package com.example.demo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request payload for resetting user password using confirmation code.
 * Used in the second step of password reset flow after receiving confirmation
 * code via email.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.controller.AuthController#resetPassword
 * @see com.example.demo.service.interfaces.CognitoService#resetPassword
 * @see com.example.demo.dto.request.ForgotPasswordRequest
 */
@Data
public class ResetPasswordRequest {

    /**
     * Email address of the user resetting their password.
     * Must match the email used in the forgot password request.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    /**
     * Confirmation code received via email from the forgot password request.
     * This code is typically 6 digits and has a limited validity period.
     */
    @NotBlank(message = "Confirmation code is required")
    private String confirmationCode;

    /**
     * New password that will replace the current password.
     * Must meet the password policy requirements configured in Cognito User Pool.
     */
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String newPassword;
}
