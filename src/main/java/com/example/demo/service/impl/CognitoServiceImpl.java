package com.example.demo.service.impl;

import com.example.demo.config.CognitoProperties;
import com.example.demo.dto.common.ApiResponse;
import com.example.demo.dto.common.CodeDeliveryResponse;
import com.example.demo.dto.request.*;
import com.example.demo.dto.response.*;
import com.example.demo.service.interfaces.CognitoService;
import com.example.demo.service.interfaces.RateLimitingService;
import com.example.demo.util.AuthLogger;
import com.example.demo.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of CognitoService for AWS Cognito User Pool operations.
 * Provides comprehensive authentication and user management functionality with
 * built-in security features including rate limiting, validation, and audit
 * logging.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.service.interfaces.CognitoService
 * @see com.example.demo.service.interfaces.RateLimitingService
 * @see com.example.demo.util.ValidationUtils
 * @see com.example.demo.util.AuthLogger
 */
@Slf4j
@Service
public class CognitoServiceImpl implements CognitoService {

    @Autowired
    private CognitoIdentityProviderClient cognitoClient;

    @Autowired
    private CognitoProperties cognitoProperties;

    @Autowired
    private RateLimitingService rateLimitingService;

    @Autowired
    private ValidationUtils validationUtils;

    @Autowired
    private AuthLogger authLogger;

    /**
     * Calculates the SECRET_HASH required for AWS Cognito client operations.
     * Uses HMAC-SHA256 algorithm with the client secret and username.
     * 
     * @param username The username for hash calculation
     * @return Base64-encoded SECRET_HASH
     * @throws RuntimeException if hash calculation fails
     */
    private String calculateSecretHash(String username) {
        try {
            final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
            SecretKeySpec signingKey = new SecretKeySpec(
                    cognitoProperties.getClientSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            mac.init(signingKey);
            mac.update(username.getBytes(StandardCharsets.UTF_8));
            byte[] rawHmac = mac.doFinal(cognitoProperties.getClientId().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error calculating secret hash", e);
        }
    }

    @Override
    public ApiResponse<Object> signUp(com.example.demo.dto.request.SignUpRequest request) {
        try {
            Map<String, String> attributes = new HashMap<>();
            attributes.put("email", request.getEmail());
            attributes.put("name", request.getName());

            software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest.Builder signUpRequestBuilder = software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest
                    .builder()
                    .clientId(cognitoProperties.getClientId())
                    .username(request.getUsername()) // Use username
                    .password(request.getPassword())
                    .secretHash(calculateSecretHash(request.getUsername())) // Calculate hash with username
                    .userAttributes(
                            attributes.entrySet().stream()
                                    .map(entry -> AttributeType.builder()
                                            .name(entry.getKey())
                                            .value(entry.getValue())
                                            .build())
                                    .toList());

            SignUpResponse response = cognitoClient.signUp(signUpRequestBuilder.build());

            SignUpResponseData responseData = SignUpResponseData.builder()
                    .userSub(response.userSub())
                    .username(request.getUsername())
                    .codeDeliveryDetails(SignUpResponseData.CodeDeliveryDetails.builder()
                            .destination(response.codeDeliveryDetails().destination())
                            .deliveryMedium(response.codeDeliveryDetails().deliveryMedium().toString())
                            .attributeName(response.codeDeliveryDetails().attributeName())
                            .build())
                    .build();

            return ApiResponse.success(
                    "User registered successfully. Please check your email for verification code.",
                    responseData);

        } catch (UsernameExistsException e) {
            return ApiResponse.error("Username already exists", 409);
        } catch (InvalidPasswordException e) {
            return ApiResponse.error("Password does not meet requirements", 400);
        } catch (Exception e) {
            log.error("Error during sign up", e);
            return ApiResponse.error("Registration failed: " + e.getMessage(), 500);
        }
    }

    @Override
    public ApiResponse<Object> confirmSignUp(com.example.demo.dto.request.ConfirmSignUpRequest request) {
        try {
            // Use provided username, or find the actual username for the given email
            String actualUsername = request.getUsername();
            if (actualUsername == null || actualUsername.trim().isEmpty()) {
                actualUsername = findUsernameByEmail(request.getEmail());
            }

            software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpRequest.Builder confirmSignUpRequestBuilder = software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpRequest
                    .builder()
                    .clientId(cognitoProperties.getClientId())
                    .username(actualUsername) // Use actual username for confirmation
                    .confirmationCode(request.getConfirmationCode())
                    .secretHash(calculateSecretHash(actualUsername)); // Use actual username for secret hash

            cognitoClient.confirmSignUp(confirmSignUpRequestBuilder.build());

            return ApiResponse.success("User confirmed successfully");

        } catch (CodeMismatchException e) {
            return ApiResponse.error("Invalid verification code", 400);
        } catch (ExpiredCodeException e) {
            return ApiResponse.error("Verification code has expired", 400);
        } catch (Exception e) {
            log.error("Error during sign up confirmation", e);
            return ApiResponse.error("Confirmation failed: " + e.getMessage(), 500);
        }
    }

    @Override
    public AuthResponse signIn(com.example.demo.dto.request.SignInRequest request) {
        try {
            // Generate a unique session ID
            String sessionId = java.util.UUID.randomUUID().toString();

            // Get login identifier (username or email)
            String loginIdentifier = request.getLogin();

            // For aliases (email), find the actual username for secret hash
            String usernameForSecretHash = loginIdentifier;
            if (loginIdentifier.contains("@")) {
                // If it's an email, we need to find the username or use email for secret hash
                // Since we're using email as alias, try with email first
                usernameForSecretHash = loginIdentifier;
            }

            // First try USER_PASSWORD_AUTH
            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", loginIdentifier);
            authParams.put("PASSWORD", request.getPassword());
            authParams.put("SECRET_HASH", calculateSecretHash(usernameForSecretHash));

            try {
                InitiateAuthRequest.Builder authRequestBuilder = InitiateAuthRequest.builder()
                        .clientId(cognitoProperties.getClientId())
                        .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                        .authParameters(authParams);

                InitiateAuthResponse response = cognitoClient.initiateAuth(authRequestBuilder.build());

                if (response.challengeName() != null) {
                    return AuthResponse.challenge(
                            "Challenge required: " + response.challengeName(),
                            response.challengeName().toString(),
                            response.session(),
                            sessionId);
                }

                AuthenticationResultType authResult = response.authenticationResult();
                return AuthResponse.success(
                        "Sign in successful",
                        authResult.accessToken(),
                        authResult.refreshToken(),
                        authResult.idToken(),
                        sessionId);
            } catch (Exception e) {
                if (e.getMessage().contains("flow not enabled")) {
                    // If USER_PASSWORD_AUTH is not enabled, try ADMIN_USER_PASSWORD_AUTH
                    AdminInitiateAuthRequest.Builder adminAuthRequestBuilder = AdminInitiateAuthRequest.builder()
                            .userPoolId(cognitoProperties.getUserPoolId())
                            .clientId(cognitoProperties.getClientId())
                            .authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
                            .authParameters(authParams);

                    AdminInitiateAuthResponse adminResponse = cognitoClient
                            .adminInitiateAuth(adminAuthRequestBuilder.build());

                    if (adminResponse.challengeName() != null) {
                        return AuthResponse.challenge(
                                "Challenge required: " + adminResponse.challengeName(),
                                adminResponse.challengeName().toString(),
                                adminResponse.session(),
                                sessionId);
                    }

                    AuthenticationResultType authResult = adminResponse.authenticationResult();
                    return AuthResponse.success(
                            "Sign in successful",
                            authResult.accessToken(),
                            authResult.refreshToken(),
                            authResult.idToken(),
                            sessionId);
                } else {
                    throw e;
                }
            }

        } catch (NotAuthorizedException e) {
            return AuthResponse.error("Invalid login credentials", 401);
        } catch (UserNotConfirmedException e) {
            return AuthResponse.error("User not confirmed. Please verify your email.", 400);
        } catch (Exception e) {
            log.error("Error during sign in", e);
            return AuthResponse.error("Sign in failed: " + e.getMessage(), 500);
        }
    }

    /**
     * Initiates the forgot password process by sending a password reset code to
     * user's email.
     * Includes comprehensive validation, rate limiting, and security checks.
     * 
     * Features:
     * - Email format validation
     * - Suspicious email detection
     * - Rate limiting (5 requests per hour)
     * - Audit logging
     * - Standardized error handling
     * 
     * @param request Contains the user's email address
     * @return ApiResponse with code delivery details or error information
     * @throws UserNotFoundException     if user does not exist
     * @throws InvalidParameterException if email format is invalid
     */
    @Override
    public ApiResponse<Object> forgotPassword(com.example.demo.dto.request.ForgotPasswordRequest request) {
        // Validate email format
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return ApiResponse.error("Email is required", 400);
        }

        String email = validationUtils.normalizeEmail(request.getEmail());

        // Additional validation checks
        if (!validationUtils.isValidEmail(email)) {
            authLogger.logValidationFailure("FORGOT_PASSWORD", email, "system", "Invalid email format");
            return ApiResponse.error("Invalid email format", 400);
        }

        if (validationUtils.isSuspiciousEmail(email)) {
            authLogger.logSecurityEvent("SUSPICIOUS_EMAIL", email, "system", "Disposable or suspicious email pattern");
            return ApiResponse.error("Email address not allowed", 400);
        }

        return sendCodeWithRateLimit(email, "FORGOT_PASSWORD", "PASSWORD_RESET", () -> {
            software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordRequest.Builder forgotPasswordRequestBuilder = software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordRequest
                    .builder()
                    .clientId(cognitoProperties.getClientId())
                    .username(email)
                    .secretHash(calculateSecretHash(email));

            ForgotPasswordResponse response = cognitoClient.forgotPassword(forgotPasswordRequestBuilder.build());
            return response.codeDeliveryDetails();
        });
    }

    @Override
    public ApiResponse<Object> resetPassword(com.example.demo.dto.request.ResetPasswordRequest request) {
        try {
            ConfirmForgotPasswordRequest.Builder confirmForgotPasswordRequestBuilder = ConfirmForgotPasswordRequest
                    .builder()
                    .clientId(cognitoProperties.getClientId())
                    .username(request.getEmail())
                    .confirmationCode(request.getConfirmationCode())
                    .password(request.getNewPassword())
                    .secretHash(calculateSecretHash(request.getEmail()));

            cognitoClient.confirmForgotPassword(confirmForgotPasswordRequestBuilder.build());

            return ApiResponse.success("Password reset successfully");

        } catch (CodeMismatchException e) {
            return ApiResponse.error("Invalid reset code", 400);
        } catch (ExpiredCodeException e) {
            return ApiResponse.error("Reset code has expired", 400);
        } catch (InvalidPasswordException e) {
            return ApiResponse.error("Password does not meet requirements", 400);
        } catch (Exception e) {
            log.error("Error during password reset", e);
            return ApiResponse.error("Password reset failed: " + e.getMessage(), 500);
        }
    }

    /**
     * Resends confirmation code to user's email for account verification.
     * Includes comprehensive validation, rate limiting, and security checks.
     * 
     * Features:
     * - Email format validation
     * - Suspicious email detection
     * - Rate limiting (3 requests per hour)
     * - Audit logging
     * - Standardized error handling
     * 
     * @param request Contains the user's email address
     * @return ApiResponse with code delivery details or error information
     * @throws UserNotFoundException     if user does not exist
     * @throws InvalidParameterException if email format is invalid
     */
    @Override
    public ApiResponse<Object> resendConfirmationCode(ResendCodeRequest request) {
        // Validate email format
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return ApiResponse.error("Email is required", 400);
        }

        String email = validationUtils.normalizeEmail(request.getEmail());

        // Additional validation checks
        if (!validationUtils.isValidEmail(email)) {
            authLogger.logValidationFailure("RESEND_CONFIRMATION", email, "system", "Invalid email format");
            return ApiResponse.error("Invalid email format", 400);
        }

        if (validationUtils.isSuspiciousEmail(email)) {
            authLogger.logSecurityEvent("SUSPICIOUS_EMAIL", email, "system", "Disposable or suspicious email pattern");
            return ApiResponse.error("Email address not allowed", 400);
        }

        return sendCodeWithRateLimit(email, "RESEND_CONFIRMATION", "CONFIRMATION", () -> {
            ResendConfirmationCodeRequest.Builder resendRequestBuilder = ResendConfirmationCodeRequest.builder()
                    .clientId(cognitoProperties.getClientId())
                    .username(email)
                    .secretHash(calculateSecretHash(email));

            ResendConfirmationCodeResponse response = cognitoClient
                    .resendConfirmationCode(resendRequestBuilder.build());
            return response.codeDeliveryDetails();
        });
    }

    @Override
    public ApiResponse<ProfileResponse> getUserProfile(String accessToken) {
        try {
            GetUserRequest getUserRequest = GetUserRequest.builder()
                    .accessToken(accessToken)
                    .build();

            GetUserResponse response = cognitoClient.getUser(getUserRequest);

            // Extract user attributes
            String email = null;
            String name = null;
            Boolean emailVerified = null;

            for (AttributeType attribute : response.userAttributes()) {
                log.debug("Attribute: {} = {}", attribute.name(), attribute.value());
                switch (attribute.name()) {
                    case "email":
                        email = attribute.value();
                        break;
                    case "name":
                        name = attribute.value();
                        break;
                    case "email_verified":
                        emailVerified = Boolean.parseBoolean(attribute.value());
                        break;
                }
            }

            ProfileResponse.ProfileResponseBuilder profileBuilder = ProfileResponse.builder()
                    .userId(response.username()) // Use username as userId
                    .username(response.username())
                    .userStatus("ACTIVE");

            // Only set non-null values
            if (email != null) {
                profileBuilder.email(email);
            }
            if (name != null) {
                profileBuilder.name(name);
            }
            if (emailVerified != null) {
                profileBuilder.emailVerified(emailVerified);
            }

            return ApiResponse.success("Profile retrieved successfully", profileBuilder.build());

        } catch (NotAuthorizedException e) {
            return ApiResponse.error("Invalid or expired access token", 401);
        } catch (Exception e) {
            log.error("Error retrieving user profile", e);
            return ApiResponse.error("Failed to retrieve profile: " + e.getMessage(), 500);
        }
    }

    // Refresh tokens using refresh token
    @Override
    public ApiResponse<Object> refreshTokens(String refreshToken) {
        return refreshTokens(refreshToken, null);
    }

    // Overloaded method that accepts username for SECRET_HASH
    @Override
    public ApiResponse<Object> refreshTokens(String refreshToken, String username) {
        try {
            Map<String, String> authParams = new HashMap<>();
            authParams.put("REFRESH_TOKEN", refreshToken);

            // Add SECRET_HASH if username is provided
            if (username != null && !username.trim().isEmpty()) {
                authParams.put("SECRET_HASH", calculateSecretHash(username));
            }

            InitiateAuthRequest.Builder authRequestBuilder = InitiateAuthRequest.builder()
                    .clientId(cognitoProperties.getClientId())
                    .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                    .authParameters(authParams);

            InitiateAuthResponse response = cognitoClient.initiateAuth(authRequestBuilder.build());
            AuthenticationResultType authResult = response.authenticationResult();

            RefreshTokenResponseData responseData = RefreshTokenResponseData.builder()
                    .accessToken(authResult.accessToken())
                    .idToken(authResult.idToken() != null ? authResult.idToken() : "")
                    .tokenType("Bearer")
                    .build();

            return ApiResponse.success("Tokens refreshed successfully", responseData);

        } catch (Exception e) {
            log.error("Error refreshing tokens", e);

            if (e.getMessage().contains("SecretHash")) {
                return ApiResponse.error(
                        "Refresh token failed: SECRET_HASH is required. Please provide the username or sign in again.",
                        400);
            }

            return ApiResponse.error("Failed to refresh tokens: " + e.getMessage(), 401);
        }
    }

    // Global sign out
    @Override
    public ApiResponse<Object> signOut(String accessToken) {
        try {
            GlobalSignOutRequest.Builder signOutRequestBuilder = GlobalSignOutRequest.builder()
                    .accessToken(accessToken);

            cognitoClient.globalSignOut(signOutRequestBuilder.build());

            return ApiResponse.success("Successfully signed out");

        } catch (Exception e) {
            log.error("Error during sign out", e);
            return ApiResponse.error("Sign out failed: " + e.getMessage(), 500);
        }
    }

    // Validate session
    @Override
    public ApiResponse<Object> validateSession(String accessToken) {
        try {
            GetUserRequest.Builder getUserRequestBuilder = GetUserRequest.builder()
                    .accessToken(accessToken);

            GetUserResponse response = cognitoClient.getUser(getUserRequestBuilder.build());

            return ApiResponse.success(
                    "Session is valid",
                    Map.of(
                            "username", response.username(),
                            "userStatus", "ACTIVE",
                            "isValid", true));

        } catch (Exception e) {
            log.error("Error validating session", e);
            return ApiResponse.error("Invalid session: " + e.getMessage(), 401);
        }
    }

    // Helper method to find username by email
    private String findUsernameByEmail(String email) {
        try {
            ListUsersRequest listRequest = ListUsersRequest.builder()
                    .userPoolId(cognitoProperties.getUserPoolId())
                    .filter("email = \"" + email + "\"")
                    .build();

            ListUsersResponse listResponse = cognitoClient.listUsers(listRequest);

            if (!listResponse.users().isEmpty()) {
                return listResponse.users().get(0).username();
            }
        } catch (Exception e) {
            log.warn("Could not find username for email: " + email, e);
        }
        return email; // fallback to email
    }

    /**
     * Helper method to handle code delivery operations with integrated rate
     * limiting and security checks.
     * Provides a unified approach for sending confirmation codes and password reset
     * codes.
     * 
     * @param email      User's email address (normalized)
     * @param action     Action type for rate limiting ("FORGOT_PASSWORD" or
     *                   "RESEND_CONFIRMATION")
     * @param codeType   Type of code being sent ("PASSWORD_RESET" or
     *                   "CONFIRMATION")
     * @param codeSender Functional interface to execute the actual code sending
     *                   operation
     * @return ApiResponse with success/failure information and code delivery
     *         details
     * @see #forgotPassword(ForgotPasswordRequest)
     * @see #resendConfirmationCode(ResendCodeRequest)
     */
    private ApiResponse<Object> sendCodeWithRateLimit(String email, String action, String codeType,
            CodeSender codeSender) {
        // Check rate limiting
        if (rateLimitingService.isRateLimited(email, action)) {
            long remainingTime = rateLimitingService.getRemainingTime(email, action);
            String message = String.format("Too many requests. Please try again in %d minutes.",
                    (remainingTime / 60) + 1);

            authLogger.logRateLimit(action, email, "system", remainingTime);
            return ApiResponse.error(message, 429);
        }

        try {
            // Execute the code sending operation
            var response = codeSender.sendCode();

            // Record the action for rate limiting
            rateLimitingService.recordAction(email, action);

            // Build standardized response
            CodeDeliveryResponse codeDeliveryResponse = CodeDeliveryResponse.builder()
                    .destination(response.destination())
                    .deliveryMedium(response.deliveryMedium().toString())
                    .attributeName(response.attributeName())
                    .codeType(codeType)
                    .build();

            String message = codeType.equals("CONFIRMATION")
                    ? "Confirmation code sent successfully"
                    : "Password reset code sent successfully";

            authLogger.logAuditEvent(action, email, "system", "AWS-Cognito", "SUCCESS",
                    "Code sent to " + response.destination());

            return ApiResponse.success(message, codeDeliveryResponse);

        } catch (UserNotFoundException e) {
            authLogger.logAuthFailure(action, email, "system", "User not found");
            return ApiResponse.error("User not found", 404);
        } catch (InvalidParameterException e) {
            authLogger.logValidationFailure(action, email, "system", "Invalid email parameter");
            return ApiResponse.error("Invalid email address", 400);
        } catch (Exception e) {
            authLogger.logAuthError(action, email, e);
            return ApiResponse.error("Failed to send code: " + e.getMessage(), 500);
        }
    }

    /**
     * Functional interface for abstracting code sending operations.
     * Allows for clean separation of rate limiting logic from AWS SDK calls.
     */
    @FunctionalInterface
    private interface CodeSender {
        /**
         * Executes the code sending operation.
         * 
         * @return CodeDeliveryDetailsType from AWS Cognito response
         * @throws Exception if the operation fails
         */
        CodeDeliveryDetailsType sendCode() throws Exception;
    }
}
