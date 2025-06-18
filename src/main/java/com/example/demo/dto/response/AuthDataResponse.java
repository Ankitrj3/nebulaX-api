package com.example.demo.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data container for authentication response information.
 * Contains JWT tokens, session details, and challenge information for
 * authentication flows.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.dto.response.AuthResponse
 * @see com.example.demo.service.interfaces.CognitoService#signIn
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AuthDataResponse {

    /** JWT access token for API authorization (short-lived, typically 1 hour) */
    private String accessToken;

    /**
     * Refresh token for obtaining new access tokens (long-lived, typically 30 days)
     */
    private String refreshToken;

    /** JWT ID token containing user profile information */
    private String idToken;

    /** Unique session identifier for tracking user sessions */
    private String sessionId;

    /** Challenge type name for multi-factor authentication flows */
    private String challengeName;

    /** Challenge session token for completing authentication challenges */
    private String session;
}
