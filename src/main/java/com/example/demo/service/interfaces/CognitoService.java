package com.example.demo.service.interfaces;

import com.example.demo.dto.common.ApiResponse;
import com.example.demo.dto.request.*;
import com.example.demo.dto.response.AuthResponse;
import com.example.demo.dto.response.ProfileResponse;

/**
 * Service interface for AWS Cognito authentication and user management
 * operations.
 * Provides comprehensive user lifecycle management including registration,
 * authentication,
 * password management, and profile operations.
 * 
 * Features:
 * - User registration and email verification
 * - Authentication with username/email and password
 * - Password reset and recovery
 * - Token refresh and session management
 * - User profile retrieval
 * - Rate limiting integration
 * - Security validation and logging
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.service.impl.CognitoServiceImpl
 * @see com.example.demo.controller.AuthController
 */
public interface CognitoService {

    /**
     * Registers a new user in the Cognito User Pool.
     * 
     * @param request SignUpRequest containing user registration details
     * @return ApiResponse with registration result and confirmation code delivery
     *         details
     */
    ApiResponse<Object> signUp(SignUpRequest request);

    /**
     * Confirms user registration using the verification code sent to their email.
     * 
     * @param request ConfirmSignUpRequest containing email and confirmation code
     * @return ApiResponse with confirmation result
     */
    ApiResponse<Object> confirmSignUp(ConfirmSignUpRequest request);

    /**
     * Authenticates user credentials and returns authentication tokens.
     * 
     * @param request SignInRequest containing login credentials (username/email and
     *                password)
     * @return AuthResponse with authentication tokens or challenge information
     */
    AuthResponse signIn(SignInRequest request);

    /**
     * Initiates password reset flow by sending confirmation code to user's email.
     * Rate limited to prevent abuse.
     * 
     * @param request ForgotPasswordRequest containing user's email address
     * @return ApiResponse with code delivery confirmation
     */
    ApiResponse<Object> forgotPassword(ForgotPasswordRequest request);

    /**
     * Completes password reset using confirmation code and new password.
     * 
     * @param request ResetPasswordRequest containing email, code, and new password
     * @return ApiResponse with password reset confirmation
     */
    ApiResponse<Object> resetPassword(ResetPasswordRequest request);

    /**
     * Resends confirmation code to user's email address.
     * Rate limited to prevent abuse.
     * 
     * @param request ResendCodeRequest containing user's email address
     * @return ApiResponse with code delivery confirmation
     */
    ApiResponse<Object> resendConfirmationCode(ResendCodeRequest request);

    /**
     * Retrieves user profile information using access token.
     * 
     * @param accessToken Valid JWT access token
     * @return ApiResponse containing user profile data
     */
    ApiResponse<ProfileResponse> getUserProfile(String accessToken);

    /**
     * Refreshes access tokens using refresh token.
     * 
     * @param refreshToken Valid refresh token
     * @return ApiResponse with new access and ID tokens
     */
    ApiResponse<Object> refreshTokens(String refreshToken);

    /**
     * Refreshes access tokens using refresh token with username for SECRET_HASH
     * calculation.
     * 
     * @param refreshToken Valid refresh token
     * @param username     Username for SECRET_HASH calculation (required for some
     *                     User Pool configurations)
     * @return ApiResponse with new access and ID tokens
     */
    ApiResponse<Object> refreshTokens(String refreshToken, String username);

    /**
     * Signs out user from all devices (global sign out).
     * 
     * @param accessToken Valid JWT access token
     * @return ApiResponse with sign out confirmation
     */
    ApiResponse<Object> signOut(String accessToken);

    /**
     * Validate session/token
     */
    ApiResponse<Object> validateSession(String accessToken);
}
