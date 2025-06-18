package com.example.demo.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response data object for token refresh operations.
 * Contains new tokens obtained after refreshing authentication.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.controller.AuthController#refreshToken
 * @see com.example.demo.service.interfaces.CognitoService#refreshTokens
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RefreshTokenResponseData {

    /** New JWT access token for API authorization */
    private String accessToken;

    /** New JWT ID token containing user claims */
    private String idToken;

    /** Token type (typically "Bearer") */
    private String tokenType;
}
