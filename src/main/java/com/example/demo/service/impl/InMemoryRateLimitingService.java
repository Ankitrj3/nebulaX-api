package com.example.demo.service.impl;

import com.example.demo.service.interfaces.RateLimitingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * In-memory implementation of rate limiting service.
 * Uses concurrent hash maps to track request counts per identifier and action
 * type.
 * Suitable for single-instance deployments or development environments.
 * For production multi-instance deployments, consider Redis-based
 * implementation.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.service.interfaces.RateLimitingService
 */
@Slf4j
@Service
public class InMemoryRateLimitingService implements RateLimitingService {

    /** Thread-safe storage for rate limit tracking data */
    private final Map<String, Map<String, RateLimitInfo>> rateLimitData = new ConcurrentHashMap<>();

    // Rate limits configuration - can be externalized to application properties
    /** Maximum forgot password requests per time window */
    private static final int FORGOT_PASSWORD_LIMIT = 5; // 5 requests per hour

    /** Maximum resend confirmation requests per time window */
    private static final int RESEND_CONFIRMATION_LIMIT = 3; // 3 requests per hour

    /** Time window for rate limiting in seconds (1 hour) */
    private static final long RATE_LIMIT_WINDOW_SECONDS = 3600; // 1 hour

    /**
     * Internal class to store rate limiting information for each identifier-action
     * combination.
     */
    private static class RateLimitInfo {
        private int count;
        private long windowStart;

        /**
         * Creates a new rate limit info entry.
         * 
         * @param count       Initial request count
         * @param windowStart Timestamp when the current window started
         */
        public RateLimitInfo(int count, long windowStart) {
            this.count = count;
            this.windowStart = windowStart;
        }

        public int getCount() {
            return count;
        }

        public long getWindowStart() {
            return windowStart;
        }

        public void incrementCount() {
            this.count++;
        }

        /**
         * Resets the rate limit window with a new start time.
         * 
         * @param newWindowStart New window start timestamp
         */
        public void reset(long newWindowStart) {
            this.count = 1;
            this.windowStart = newWindowStart;
        }
    }

    @Override
    public boolean isRateLimited(String identifier, String action) {
        String key = identifier.toLowerCase();
        Map<String, RateLimitInfo> userLimits = rateLimitData.computeIfAbsent(key, k -> new ConcurrentHashMap<>());

        RateLimitInfo info = userLimits.get(action);
        if (info == null) {
            return false;
        }

        long currentTime = Instant.now().getEpochSecond();
        long timeSinceWindowStart = currentTime - info.getWindowStart();

        // Reset window if it's expired
        if (timeSinceWindowStart >= RATE_LIMIT_WINDOW_SECONDS) {
            return false;
        }

        int limit = getLimit(action);
        boolean isLimited = info.getCount() >= limit;

        if (isLimited) {
            log.debug("Rate limit check: identifier={}, action={}, count={}/{}, limited={}",
                    key, action, info.getCount(), limit, isLimited);
        }

        return isLimited;
    }

    @Override
    public void recordAction(String identifier, String action) {
        String key = identifier.toLowerCase();
        Map<String, RateLimitInfo> userLimits = rateLimitData.computeIfAbsent(key, k -> new ConcurrentHashMap<>());

        long currentTime = Instant.now().getEpochSecond();
        RateLimitInfo info = userLimits.get(action);

        if (info == null) {
            userLimits.put(action, new RateLimitInfo(1, currentTime));
            log.debug("New rate limit entry created: identifier={}, action={}, count=1", key, action);
        } else {
            long timeSinceWindowStart = currentTime - info.getWindowStart();

            if (timeSinceWindowStart >= RATE_LIMIT_WINDOW_SECONDS) {
                // Reset window
                info.reset(currentTime);
                log.debug("Rate limit window reset: identifier={}, action={}, count=1", key, action);
            } else {
                // Increment count in current window
                info.incrementCount();
                log.debug("Rate limit count incremented: identifier={}, action={}, count={}",
                        key, action, info.getCount());
            }
        }
    }

    @Override
    public long getRemainingTime(String identifier, String action) {
        String key = identifier.toLowerCase();
        Map<String, RateLimitInfo> userLimits = rateLimitData.get(key);

        if (userLimits == null) {
            return 0;
        }

        RateLimitInfo info = userLimits.get(action);
        if (info == null) {
            return 0;
        }

        long currentTime = Instant.now().getEpochSecond();
        long timeSinceWindowStart = currentTime - info.getWindowStart();

        if (timeSinceWindowStart >= RATE_LIMIT_WINDOW_SECONDS) {
            return 0;
        }

        return RATE_LIMIT_WINDOW_SECONDS - timeSinceWindowStart;
    }

    /**
     * Gets the configured rate limit for a specific action type.
     * 
     * @param action The action to get the limit for
     * @return The maximum number of requests allowed per time window
     */
    private int getLimit(String action) {
        return switch (action) {
            case "FORGOT_PASSWORD" -> FORGOT_PASSWORD_LIMIT;
            case "RESEND_CONFIRMATION" -> RESEND_CONFIRMATION_LIMIT;
            default -> 10; // default limit for unknown actions
        };
    }

    /**
     * Clears all rate limiting data. Useful for testing or administrative purposes.
     * WARNING: This will reset all rate limits for all users.
     */
    public void clearAllRateLimits() {
        rateLimitData.clear();
        log.info("All rate limiting data cleared");
    }

    /**
     * Gets current statistics about rate limiting.
     * Useful for monitoring and debugging.
     * 
     * @return Map containing statistics about current rate limit state
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalIdentifiers", rateLimitData.size());
        stats.put("windowSizeSeconds", RATE_LIMIT_WINDOW_SECONDS);
        stats.put("forgotPasswordLimit", FORGOT_PASSWORD_LIMIT);
        stats.put("resendConfirmationLimit", RESEND_CONFIRMATION_LIMIT);
        return stats;
    }
}
