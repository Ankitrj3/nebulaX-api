package com.example.demo.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Standardized logging utility for the authentication module.
 * Provides consistent log message formats and structures across the
 * application.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.controller.AuthController
 * @see com.example.demo.service.impl.CognitoServiceImpl
 */
@Slf4j
@Component
public class AuthLogger {

    private static final String LOG_PREFIX = "[AUTH]";
    private static final String SECURITY_PREFIX = "[SECURITY]";
    private static final String RATE_LIMIT_PREFIX = "[RATE_LIMIT]";

    /**
     * Logs successful authentication operations.
     * 
     * @param operation The operation performed (e.g., "FORGOT_PASSWORD",
     *                  "RESEND_CONFIRMATION")
     * @param email     User's email address (will be masked)
     * @param ipAddress Client's IP address
     */
    public void logAuthSuccess(String operation, String email, String ipAddress) {
        log.info("{} {} operation successful - Email: {} - IP: {}",
                LOG_PREFIX, operation, maskEmail(email), ipAddress);
    }

    /**
     * Logs failed authentication operations.
     * 
     * @param operation The operation attempted
     * @param email     User's email address (will be masked)
     * @param ipAddress Client's IP address
     * @param reason    Reason for failure
     */
    public void logAuthFailure(String operation, String email, String ipAddress, String reason) {
        log.warn("{} {} operation failed - Email: {} - IP: {} - Reason: {}",
                LOG_PREFIX, operation, maskEmail(email), ipAddress, reason);
    }

    /**
     * Logs security-related events and potential threats.
     * 
     * @param event     Type of security event
     * @param email     User's email address (will be masked)
     * @param ipAddress Client's IP address
     * @param details   Additional details about the security event
     */
    public void logSecurityEvent(String event, String email, String ipAddress, String details) {
        log.warn("{} {} detected - Email: {} - IP: {} - Details: {}",
                SECURITY_PREFIX, event, maskEmail(email), ipAddress, details);
    }

    /**
     * Logs rate limiting events.
     * 
     * @param operation     The operation that was rate limited
     * @param email         User's email address (will be masked)
     * @param ipAddress     Client's IP address
     * @param remainingTime Time until rate limit resets (in seconds)
     */
    public void logRateLimit(String operation, String email, String ipAddress, long remainingTime) {
        log.warn("{} {} rate limit exceeded - Email: {} - IP: {} - Reset in: {}s",
                RATE_LIMIT_PREFIX, operation, maskEmail(email), ipAddress, remainingTime);
    }

    /**
     * Logs general authentication errors.
     * 
     * @param operation The operation being performed
     * @param email     User's email address (will be masked)
     * @param error     The error that occurred
     */
    public void logAuthError(String operation, String email, Throwable error) {
        log.error("{} {} error - Email: {} - Error: {}",
                LOG_PREFIX, operation, maskEmail(email), error.getMessage(), error);
    }

    /**
     * Logs suspicious request patterns.
     * 
     * @param ipAddress         Client's IP address
     * @param userAgent         Client's user agent string
     * @param email             User's email address (will be masked)
     * @param suspiciousPattern The pattern that was detected
     */
    public void logSuspiciousRequest(String ipAddress, String userAgent, String email, String suspiciousPattern) {
        log.warn("{} Suspicious request pattern - IP: {} - User-Agent: {} - Email: {} - Pattern: {}",
                SECURITY_PREFIX, ipAddress, userAgent, maskEmail(email), suspiciousPattern);
    }

    /**
     * Logs validation failures.
     * 
     * @param operation       The operation being performed
     * @param email           User's email address (will be masked)
     * @param ipAddress       Client's IP address
     * @param validationError The validation error that occurred
     */
    public void logValidationFailure(String operation, String email, String ipAddress, String validationError) {
        log.warn("{} {} validation failed - Email: {} - IP: {} - Validation Error: {}",
                LOG_PREFIX, operation, maskEmail(email), ipAddress, validationError);
    }

    /**
     * Masks email address for privacy while keeping it useful for debugging.
     * Example: "john.doe@example.com" becomes "j***@example.com"
     * 
     * @param email The email address to mask
     * @return Masked email address
     */
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "unknown";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "invalid_email";
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (localPart.length() <= 1) {
            return "*" + domain;
        }

        return localPart.charAt(0) + "***" + domain;
    }

    /**
     * Creates a structured log entry for audit purposes.
     * 
     * @param operation The operation performed
     * @param email     User's email (will be masked)
     * @param ipAddress Client's IP address
     * @param userAgent Client's user agent
     * @param status    Result status (SUCCESS, FAILURE, BLOCKED, etc.)
     * @param details   Additional details
     */
    public void logAuditEvent(String operation, String email, String ipAddress,
            String userAgent, String status, String details) {
        log.info("{} AUDIT - Operation: {} - Email: {} - IP: {} - User-Agent: {} - Status: {} - Details: {}",
                LOG_PREFIX, operation, maskEmail(email), ipAddress, userAgent, status, details);
    }
}
