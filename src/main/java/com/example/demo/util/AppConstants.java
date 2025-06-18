package com.example.demo.util;

/**
 * Application-wide constants for the authentication system.
 * Provides centralized management of messages, codes, and configuration values.
 * 
 * Features:
 * - Standardized response messages
 * - HTTP status messages
 * - Authentication flow messages
 * - Error codes and descriptions
 * - Configuration constants
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.dto.common.ApiResponse
 * @see com.example.demo.controller.AuthController
 */
public final class AppConstants {

    // ========================================
    // HTTP Status Messages
    // ========================================

    /** Generic success message for successful operations */
    public static final String SUCCESS_MESSAGE = "Operation completed successfully";

    /** Generic error message for failed operations */
    public static final String ERROR_MESSAGE = "An error occurred while processing the request";

    // ========================================
    // Authentication Messages
    // ========================================

    /** Success message for user sign-in */
    public static final String SIGNIN_SUCCESS = "Sign in successful";

    /** Error message for failed sign-in attempts */
    public static final String SIGNIN_FAILED = "Invalid login credentials";

    /** Success message for user registration */
    public static final String SIGNUP_SUCCESS = "User registered successfully. Please check your email for verification code.";

    /** Success message for account confirmation */
    public static final String CONFIRMATION_SUCCESS = "User confirmed successfully";

    /** Success message for user sign-out */
    public static final String SIGNOUT_SUCCESS = "Successfully signed out";

    /** Message for valid session validation */
    public static final String SESSION_VALID = "Session is valid";

    /** Error message for invalid or expired sessions */
    public static final String SESSION_INVALID = "Invalid or expired access token";

    /** Success message for profile retrieval */
    public static final String PROFILE_SUCCESS = "Profile retrieved successfully";

    /** Success message for token refresh */
    public static final String TOKENS_REFRESHED = "Tokens refreshed successfully";

    /** Success message for password reset code sending */
    public static final String PASSWORD_RESET_SENT = "Password reset code sent to your email";

    /** Success message for password reset completion */
    public static final String PASSWORD_RESET_SUCCESS = "Password reset successfully";

    /** Success message for confirmation code resending */
    public static final String CONFIRMATION_RESENT = "Confirmation code resent successfully";

    // ========================================
    // Validation Messages
    // ========================================

    /** Generic validation failure message */
    public static final String VALIDATION_FAILED = "Validation failed";

    /** Required field validation message */
    public static final String REQUIRED_FIELD = "This field is required";

    /** Invalid email format message */
    public static final String INVALID_EMAIL = "Email should be valid";

    /** Password requirements not met message */
    public static final String INVALID_PASSWORD = "Password does not meet requirements";

    // ========================================
    // Security Messages
    // ========================================

    /** Rate limit exceeded message */
    public static final String RATE_LIMIT_EXCEEDED = "Too many requests. Please try again later.";

    /** Suspicious request blocked message */
    public static final String SECURITY_BLOCK = "Request blocked for security reasons";

    /** Disposable email blocked message */
    public static final String DISPOSABLE_EMAIL_BLOCKED = "Email address not allowed";

    /** Suspicious email pattern detected message */
    public static final String SUSPICIOUS_EMAIL = "Email address appears suspicious";

    // ========================================
    // Health Check Messages
    // ========================================

    /** Health check success message */
    public static final String HEALTH_CHECK_SUCCESS = "Health check successful";

    /** Health check failure message */
    public static final String HEALTH_CHECK_FAILED = "Health check failed";

    /** Application healthy status */
    public static final String HEALTH_STATUS_UP = "UP";

    /** Application unhealthy status */
    public static final String HEALTH_STATUS_DOWN = "DOWN";

    // ========================================
    // HTTP Headers and Authentication
    // ========================================

    /** Authorization header name */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /** Bearer token prefix */
    public static final String BEARER_PREFIX = "Bearer ";

    /** User-Agent header name */
    public static final String USER_AGENT_HEADER = "User-Agent";

    /** X-Forwarded-For header name */
    public static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";

    /** X-Real-IP header name */
    public static final String X_REAL_IP_HEADER = "X-Real-IP";

    // ========================================
    // Application Configuration
    // ========================================

    /** Application name */
    public static final String APPLICATION_NAME = "Demo Authentication Application";

    /** API version */
    public static final String API_VERSION = "v2.0";

    /** Application author */
    public static final String APPLICATION_AUTHOR = "Vikas Singh";

    // ========================================
    // Rate Limiting Constants
    // ========================================

    /** Default rate limit window in seconds (1 hour) */
    public static final long DEFAULT_RATE_LIMIT_WINDOW = 3600L;

    /** Default forgot password rate limit */
    public static final int DEFAULT_FORGOT_PASSWORD_LIMIT = 5;

    /** Default resend confirmation rate limit */
    public static final int DEFAULT_RESEND_CONFIRMATION_LIMIT = 3;

    // ========================================
    // Action Types for Rate Limiting
    // ========================================

    /** Forgot password action type */
    public static final String ACTION_FORGOT_PASSWORD = "FORGOT_PASSWORD";

    /** Resend confirmation action type */
    public static final String ACTION_RESEND_CONFIRMATION = "RESEND_CONFIRMATION";

    // ========================================
    // Code Types
    // ========================================

    /** Password reset code type */
    public static final String CODE_TYPE_PASSWORD_RESET = "PASSWORD_RESET";

    /** Email confirmation code type */
    public static final String CODE_TYPE_CONFIRMATION = "CONFIRMATION";

    // ========================================
    // Delivery Mediums
    // ========================================

    /** Email delivery medium */
    public static final String DELIVERY_MEDIUM_EMAIL = "EMAIL";

    /** SMS delivery medium */
    public static final String DELIVERY_MEDIUM_SMS = "SMS";

    /**
     * Private constructor to prevent instantiation.
     * This class should only be used for its static constants.
     */
    private AppConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
