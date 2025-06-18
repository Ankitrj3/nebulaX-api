package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request payload for refreshing authentication tokens.
 * Used to obtain new access and ID tokens using a valid refresh token.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.controller.AuthController#refreshToken
 * @see com.example.demo.service.interfaces.CognitoService#refreshTokens
 */
@Data
public class RefreshTokenRequest {

    /**
     * Valid refresh token obtained during initial authentication.
     * This token has a longer expiration time than access tokens.
     */
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    /**
     * Optional username for SECRET_HASH calculation.
     * Required for Cognito User Pools that have client secret configured.
     * If not provided, the system will attempt refresh without SECRET_HASH.
     */
    private String username;
}
