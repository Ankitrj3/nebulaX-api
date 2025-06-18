package com.example.demo.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Validation utilities for authentication and security checks.
 * Provides email validation, suspicious pattern detection, and request
 * analysis.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.controller.AuthController
 * @see com.example.demo.service.impl.CognitoServiceImpl
 */
@Slf4j
@Component
public class ValidationUtils {

    /** Standard email format validation pattern */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    /** Pattern to detect suspicious email formats that might be used for spam */
    private static final Pattern SUSPICIOUS_EMAIL_PATTERN = Pattern.compile(
            ".*\\+.*@.*|.*[0-9]{10,}.*@.*");

    /**
     * Validates if the email format follows standard RFC specifications.
     * 
     * @param email The email address to validate
     * @return true if email format is valid, false otherwise
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Checks if email exhibits suspicious patterns commonly used for spam or abuse.
     * Detects patterns like email+tag@domain.com, emails with long numeric
     * sequences,
     * and known disposable email providers.
     * 
     * @param email The email address to analyze
     * @return true if email appears suspicious, false otherwise
     */
    public boolean isSuspiciousEmail(String email) {
        if (email == null) {
            return true;
        }

        String normalizedEmail = email.trim().toLowerCase();

        // Check for suspicious patterns
        if (SUSPICIOUS_EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            log.warn("Suspicious email pattern detected: {}", normalizedEmail);
            return true;
        }

        // Check for common disposable email domains
        String[] disposableDomains = {
                "10minutemail.com", "tempmail.org", "guerrillamail.com",
                "mailinator.com", "throwaway.email", "temp-mail.org",
                "yopmail.com", "maildrop.cc", "tempail.com"
        };

        for (String domain : disposableDomains) {
            if (normalizedEmail.endsWith("@" + domain)) {
                log.warn("Disposable email domain detected: {}", normalizedEmail);
                return true;
            }
        }

        return false;
    }

    /**
     * Normalizes email address by trimming whitespace and converting to lowercase.
     * This ensures consistent email handling throughout the application.
     * 
     * @param email The email address to normalize
     * @return Normalized email address, or null if input is null
     */
    public String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    /**
     * Analyzes request characteristics to identify potentially suspicious or
     * automated requests.
     * Checks User-Agent headers for common bot patterns and other indicators of
     * non-human traffic.
     * 
     * @param userAgent The User-Agent header from the HTTP request
     * @param ipAddress The client's IP address
     * @return true if request appears suspicious, false otherwise
     */
    public boolean isSuspiciousRequest(String userAgent, String ipAddress) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            log.warn("Request without User-Agent header from IP: {}", ipAddress);
            return true;
        }

        // Check for suspicious user agent patterns (more lenient for testing)
        String[] suspiciousPatterns = {
                "bot", "spider", "crawler", "scraper", "automated"
        };

        String lowerUserAgent = userAgent.toLowerCase();
        for (String pattern : suspiciousPatterns) {
            if (lowerUserAgent.contains(pattern)) {
                log.warn("Suspicious User-Agent detected: {} from IP: {}", userAgent, ipAddress);
                return true;
            }
        }

        return false;
    }

    /**
     * Validates that the provided string is not null, empty, or contains only
     * whitespace.
     * 
     * @param value The string to validate
     * @return true if string has meaningful content, false otherwise
     */
    public boolean hasContent(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Checks if an IP address appears to be from a known suspicious range or
     * pattern.
     * Currently performs basic validation but can be extended with threat
     * intelligence.
     * 
     * @param ipAddress The IP address to analyze
     * @return true if IP appears suspicious, false otherwise
     */
    public boolean isSuspiciousIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return true;
        }

        // Basic checks - can be extended with threat intelligence databases
        String[] suspiciousPatterns = {
                "127.0.0.1", "localhost", "0.0.0.0"
        };

        for (String pattern : suspiciousPatterns) {
            if (ipAddress.contains(pattern)) {
                log.warn("Suspicious IP address detected: {}", ipAddress);
                return true;
            }
        }

        return false;
    }
}
