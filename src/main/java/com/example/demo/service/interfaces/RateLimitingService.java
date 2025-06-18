package com.example.demo.service.interfaces;

/**
 * Service interface for implementing rate limiting functionality.
 * Provides methods to track, limit, and monitor API request rates per user/IP.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.service.impl.InMemoryRateLimitingService
 * @see com.example.demo.service.impl.CognitoServiceImpl
 */
public interface RateLimitingService {

    /**
     * Checks if the specified action is currently rate limited for the given
     * identifier.
     * 
     * @param identifier unique identifier (email, IP address, user ID, etc.)
     * @param action     the action being performed (e.g., "FORGOT_PASSWORD",
     *                   "RESEND_CONFIRMATION")
     * @return true if the action is rate limited and should be blocked, false
     *         otherwise
     */
    boolean isRateLimited(String identifier, String action);

    /**
     * Records an action occurrence for rate limiting purposes.
     * This should be called after successful validation but before processing the
     * request.
     * 
     * @param identifier unique identifier (email, IP address, user ID, etc.)
     * @param action     the action being performed (e.g., "FORGOT_PASSWORD",
     *                   "RESEND_CONFIRMATION")
     */
    void recordAction(String identifier, String action);

    /**
     * Gets the remaining time until the rate limit window resets.
     * Useful for providing meaningful error messages to users.
     * 
     * @param identifier unique identifier for the rate limit check
     * @param action     the action that is rate limited
     * @return seconds until the rate limit resets, or 0 if not currently rate
     *         limited
     */
    long getRemainingTime(String identifier, String action);
}
