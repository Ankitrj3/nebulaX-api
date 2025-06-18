package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response object for authentication operations (signin).
 * Contains authentication tokens and session information upon successful login.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.controller.AuthController#signIn
 * @see com.example.demo.service.interfaces.CognitoService#signIn
 * @see com.example.demo.dto.response.AuthDataResponse
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** Human-readable message describing the authentication result */
    private String message;

    /** Timestamp when the response was generated */
    private LocalDateTime timestamp;

    /** HTTP status code */
    private int status;

    /** Whether the authentication was successful */
    private boolean success;

    /** Authentication data containing tokens and session information */
    private AuthDataResponse data;

    /**
     * Creates a successful authentication response with tokens.
     * 
     * @param message      Success message
     * @param accessToken  JWT access token for API access
     * @param refreshToken Long-lived token for token refresh
     * @param idToken      JWT ID token containing user claims
     * @param sessionId    Unique session identifier
     * @return AuthResponse with success status and tokens
     */
    public static AuthResponse success(String message, String accessToken, String refreshToken,
            String idToken, String sessionId) {
        return AuthResponse.builder()
                .message(message)
                .timestamp(LocalDateTime.now())
                .status(200)
                .success(true)
                .data(AuthDataResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .idToken(idToken)
                        .sessionId(sessionId)
                        .build())
                .build();
    }

    /**
     * Creates a challenge response for multi-factor authentication or other
     * challenges.
     * 
     * @param message       Challenge description message
     * @param challengeName Type of challenge required (e.g., "SMS_MFA",
     *                      "SOFTWARE_TOKEN_MFA")
     * @param session       Challenge session token
     * @param sessionId     Unique session identifier
     * @return AuthResponse with challenge information
     */
    public static AuthResponse challenge(String message, String challengeName, String session, String sessionId) {
        return AuthResponse.builder()
                .message(message)
                .timestamp(LocalDateTime.now())
                .status(200)
                .success(false)
                .data(AuthDataResponse.builder()
                        .sessionId(sessionId)
                        .challengeName(challengeName)
                        .session(session)
                        .build())
                .build();
    }

    /**
     * Creates an error response for failed authentication attempts.
     * 
     * @param message Error description
     * @param status  HTTP status code
     * @return AuthResponse with error information
     */
    public static AuthResponse error(String message, int status) {
        return AuthResponse.builder()
                .message(message)
                .timestamp(LocalDateTime.now())
                .status(status)
                .success(false)
                .build();
    }
}
