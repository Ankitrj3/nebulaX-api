package com.example.demo.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response object containing user profile information.
 * Retrieved using a valid access token and contains user attributes from
 * Cognito User Pool.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.controller.AuthController#getUserProfile
 * @see com.example.demo.service.interfaces.CognitoService#getUserProfile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Only include non-null fields
public class ProfileResponse {

    /** Unique user identifier from Cognito User Pool */
    private String userId;

    /** Username chosen during registration */
    private String username;

    /** Full name of the user */
    private String name;

    /** Email address associated with the account */
    private String email;

    /** Whether the email address has been verified */
    private Boolean emailVerified;

    /** Current status of the user account (CONFIRMED, UNCONFIRMED, etc.) */
    private String userStatus;
}
