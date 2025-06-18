package com.example.demo.controller;

import com.example.demo.dto.common.ApiResponse;
import com.example.demo.dto.request.*;
import com.example.demo.dto.response.AuthResponse;
import com.example.demo.dto.response.ProfileResponse;
import com.example.demo.service.interfaces.CognitoService;
import com.example.demo.util.AuthLogger;
import com.example.demo.util.ValidationUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for authentication and user management operations.
 * Provides endpoints for user registration, login, password management, and
 * profile access.
 * Includes security features like rate limiting, request validation, and audit
 * logging.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.service.interfaces.CognitoService
 * @see com.example.demo.util.ValidationUtils
 * @see com.example.demo.util.AuthLogger
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private CognitoService cognitoService;

    @Autowired
    private ValidationUtils validationUtils;

    @Autowired
    private AuthLogger authLogger;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Object>> signUp(@Valid @RequestBody SignUpRequest request) {
        log.info("Sign up request for email: {}", request.getEmail());
        ApiResponse<Object> response = cognitoService.signUp(request);
        HttpStatus status = response.isSuccess() ? HttpStatus.CREATED : HttpStatus.valueOf(response.getStatus());
        return new ResponseEntity<>(response, status);
    }

    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> signIn(@Valid @RequestBody SignInRequest request) {
        log.info("Sign in request for login: {}", request.getLogin());
        AuthResponse response = cognitoService.signIn(request);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.valueOf(response.getStatus());
        return new ResponseEntity<>(response, status);
    }

    @PostMapping("/confirm-signup")
    public ResponseEntity<ApiResponse<Object>> confirmSignUp(@Valid @RequestBody ConfirmSignUpRequest request) {
        log.info("Confirm sign up request for email: {}", request.getEmail());
        ApiResponse<Object> response = cognitoService.confirmSignUp(request);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.valueOf(response.getStatus());
        return new ResponseEntity<>(response, status);
    }

    /**
     * Initiates the password reset process by sending a reset code to the user's
     * email.
     * Includes rate limiting, email validation, and security checks to prevent
     * abuse.
     * 
     * @param request     Contains the email address for password reset
     * @param httpRequest HTTP request for security analysis
     * @return ApiResponse containing code delivery information or error details
     * @see com.example.demo.service.interfaces.CognitoService#forgotPassword
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Object>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        // Security checks
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(httpRequest);

        if (validationUtils.isSuspiciousRequest(userAgent, ipAddress)) {
            authLogger.logSecurityEvent("SUSPICIOUS_REQUEST_BLOCKED", request.getEmail(), ipAddress,
                    "User-Agent: " + userAgent);
            return new ResponseEntity<>(
                    ApiResponse.error("Request blocked for security reasons", 403),
                    HttpStatus.FORBIDDEN);
        }

        authLogger.logAuditEvent("FORGOT_PASSWORD", request.getEmail(), ipAddress, userAgent, "INITIATED",
                "Request received");

        ApiResponse<Object> response = cognitoService.forgotPassword(request);

        if (response.isSuccess()) {
            authLogger.logAuthSuccess("FORGOT_PASSWORD", request.getEmail(), ipAddress);
        } else {
            authLogger.logAuthFailure("FORGOT_PASSWORD", request.getEmail(), ipAddress, response.getMessage());
        }

        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.valueOf(response.getStatus());
        return new ResponseEntity<>(response, status);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset password request for email: {}", request.getEmail());
        ApiResponse<Object> response = cognitoService.resetPassword(request);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.valueOf(response.getStatus());
        return new ResponseEntity<>(response, status);
    }

    /**
     * Resends confirmation code to user's email for account verification.
     * Includes rate limiting, email validation, and security checks to prevent
     * abuse.
     * 
     * @param request     Contains the email address for code resending
     * @param httpRequest HTTP request for security analysis
     * @return ApiResponse containing code delivery information or error details
     * @see com.example.demo.service.interfaces.CognitoService#resendConfirmationCode
     */
    @PostMapping("/resend-confirmation")
    public ResponseEntity<ApiResponse<Object>> resendConfirmationCode(
            @Valid @RequestBody ResendCodeRequest request,
            HttpServletRequest httpRequest) {

        // Security checks
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(httpRequest);

        if (validationUtils.isSuspiciousRequest(userAgent, ipAddress)) {
            authLogger.logSecurityEvent("SUSPICIOUS_REQUEST_BLOCKED", request.getEmail(), ipAddress,
                    "User-Agent: " + userAgent);
            return new ResponseEntity<>(
                    ApiResponse.error("Request blocked for security reasons", 403),
                    HttpStatus.FORBIDDEN);
        }

        authLogger.logAuditEvent("RESEND_CONFIRMATION", request.getEmail(), ipAddress, userAgent, "INITIATED",
                "Request received");

        ApiResponse<Object> response = cognitoService.resendConfirmationCode(request);

        if (response.isSuccess()) {
            authLogger.logAuthSuccess("RESEND_CONFIRMATION", request.getEmail(), ipAddress);
        } else {
            authLogger.logAuthFailure("RESEND_CONFIRMATION", request.getEmail(), ipAddress, response.getMessage());
        }

        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.valueOf(response.getStatus());
        return new ResponseEntity<>(response, status);
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getUserProfile(
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Extract Bearer token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ApiResponse<ProfileResponse> errorResponse = ApiResponse
                        .error("Missing or invalid Authorization header", 401);
                return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
            }

            String accessToken = authHeader.substring(7); // Remove "Bearer " prefix
            log.info("Profile request with access token");

            ApiResponse<ProfileResponse> response = cognitoService.getUserProfile(accessToken);
            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.valueOf(response.getStatus());
            return new ResponseEntity<>(response, status);

        } catch (Exception e) {
            log.error("Error in profile endpoint", e);
            ApiResponse<ProfileResponse> errorResponse = ApiResponse.error("Internal server error", 500);
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<Object>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Refresh token request");
        ApiResponse<Object> response = cognitoService.refreshTokens(request.getRefreshToken(), request.getUsername());
        HttpStatus status = response.isSuccess() ? HttpStatus.OK
                : response.getStatus() == 401 ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
        return new ResponseEntity<>(response, status);
    }

    @PostMapping("/signout")
    public ResponseEntity<ApiResponse<Object>> signOut(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ApiResponse<Object> errorResponse = ApiResponse.error("Missing or invalid Authorization header", 401);
                return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
            }

            String accessToken = authHeader.substring(7);
            log.info("Sign out request");

            ApiResponse<Object> response = cognitoService.signOut(accessToken);
            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return new ResponseEntity<>(response, status);

        } catch (Exception e) {
            log.error("Error in signout endpoint", e);
            ApiResponse<Object> errorResponse = ApiResponse.error("Internal server error", 500);
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/validate-session")
    public ResponseEntity<ApiResponse<Object>> validateSession(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ApiResponse<Object> errorResponse = ApiResponse.error("Missing or invalid Authorization header", 401);
                return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
            }

            String accessToken = authHeader.substring(7);
            log.info("Validate session request");

            ApiResponse<Object> response = cognitoService.validateSession(accessToken);
            HttpStatus status = response.isSuccess() ? HttpStatus.OK
                    : response.getStatus() == 401 ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
            return new ResponseEntity<>(response, status);

        } catch (Exception e) {
            log.error("Error in validate session endpoint", e);
            ApiResponse<Object> errorResponse = ApiResponse.error("Internal server error", 500);
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Helper method to extract client IP address from HTTP request.
     * Handles various proxy headers to get the real client IP.
     * 
     * @param request The HTTP servlet request
     * @return Client IP address as string
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP in the chain
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        // Fallback to remote address
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }
}
