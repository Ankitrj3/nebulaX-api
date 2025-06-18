package com.example.demo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request payload for resending confirmation codes to users.
 * Used when users need to receive their signup verification code again.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.controller.AuthController#resendConfirmationCode
 * @see com.example.demo.service.interfaces.CognitoService#resendConfirmationCode
 */
@Data
public class ResendCodeRequest {

    /**
     * Email address where the confirmation code should be resent.
     * Must be a valid email format and match an existing user account.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
}
