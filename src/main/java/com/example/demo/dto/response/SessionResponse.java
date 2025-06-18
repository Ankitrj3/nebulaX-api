package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response object for session management operations.
 * Contains comprehensive session information including tokens and user details.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.dto.response.AuthResponse
 * @see com.example.demo.service.interfaces.CognitoService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {

    /** Unique session identifier */
    private String sessionId;

    /** JWT access token for API authorization */
    private String accessToken;

    /** Refresh token for obtaining new access tokens */
    private String refreshToken;

    /** JWT ID token containing user claims */
    private String idToken;

    /** Timestamp when the session expires */
    private LocalDateTime expiresAt;

    /** Unique user identifier */
    private String userId;
    private String email;
    private String message;
    private LocalDateTime timestamp;
    private int status;
    private boolean success;

    public static SessionResponse success(String message, String sessionId, String accessToken,
            String refreshToken, String idToken, LocalDateTime expiresAt,
            String userId, String email) {
        return SessionResponse.builder()
                .success(true)
                .status(200)
                .message(message)
                .sessionId(sessionId)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .idToken(idToken)
                .expiresAt(expiresAt)
                .userId(userId)
                .email(email)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static SessionResponse error(String message, int status) {
        return SessionResponse.builder()
                .success(false)
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
