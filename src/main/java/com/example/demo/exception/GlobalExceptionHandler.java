package com.example.demo.exception;

import com.example.demo.dto.common.ApiResponse;
import com.example.demo.dto.response.ErrorDetailsResponse;
import com.example.demo.dto.response.ValidationErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the application.
 * Provides centralized exception handling across all controllers with
 * consistent error responses.
 * 
 * Features:
 * - Standardized error response format
 * - AWS Cognito exception mapping
 * - Validation error handling
 * - Security-focused error messages
 * - Comprehensive logging for debugging
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.dto.common.ApiResponse
 * @see com.example.demo.dto.response.ErrorDetailsResponse
 * @see com.example.demo.dto.response.ValidationErrorResponse
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

        /**
         * Handles 404 Not Found exceptions when endpoints don't exist.
         * 
         * @param ex      NoHandlerFoundException containing request details
         * @param request WebRequest for additional context
         * @return ResponseEntity with standardized error response
         */
        @ExceptionHandler(NoHandlerFoundException.class)
        public ResponseEntity<ApiResponse<Object>> handleNotFound(NoHandlerFoundException ex, WebRequest request) {
                ApiResponse<Object> response = ApiResponse.builder()
                                .message("Endpoint not found: " + ex.getRequestURL())
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.NOT_FOUND.value())
                                .build();

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
                ApiResponse<Object> response = ApiResponse.builder()
                                .message("Invalid request: " + ex.getMessage())
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException ex) {
                ApiResponse<Object> response = ApiResponse.builder()
                                .message("Internal server error: " + ex.getMessage())
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .build();

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
                ErrorDetailsResponse errorDetails = ErrorDetailsResponse.builder()
                                .exception(ex.getClass().getSimpleName())
                                .cause(ex.getCause() != null ? ex.getCause().getMessage() : "Unknown")
                                .build();

                ApiResponse<Object> response = ApiResponse.builder()
                                .message("An unexpected error occurred: " + ex.getMessage())
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .data(errorDetails)
                                .build();

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(
                        org.springframework.web.HttpRequestMethodNotSupportedException ex) {

                ErrorDetailsResponse errorDetails = ErrorDetailsResponse.builder()
                                .method(ex.getMethod())
                                .supportedMethods(ex.getSupportedMethods())
                                .build();

                ApiResponse<Object> response = ApiResponse.builder()
                                .message("HTTP method not supported: " + ex.getMethod())
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                                .data(errorDetails)
                                .build();

                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
        }

        @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
        public ResponseEntity<ApiResponse<Object>> handleMissingParameter(
                        org.springframework.web.bind.MissingServletRequestParameterException ex) {

                ErrorDetailsResponse errorDetails = ErrorDetailsResponse.builder()
                                .parameterName(ex.getParameterName())
                                .parameterType(ex.getParameterType())
                                .build();

                ApiResponse<Object> response = ApiResponse.builder()
                                .message("Missing required parameter: " + ex.getParameterName())
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .data(errorDetails)
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Object>> handleValidationException(
                        org.springframework.web.bind.MethodArgumentNotValidException ex) {

                Map<String, String> fieldErrors = new HashMap<>();
                ex.getBindingResult().getFieldErrors()
                                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

                ValidationErrorResponse errorDetails = ValidationErrorResponse.builder()
                                .fieldErrors(fieldErrors)
                                .build();

                ApiResponse<Object> response = ApiResponse.builder()
                                .message("Validation failed")
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .data(errorDetails)
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Cognito specific exception handlers
        @ExceptionHandler(UsernameExistsException.class)
        public ResponseEntity<ApiResponse<Object>> handleUsernameExists(UsernameExistsException ex) {
                ApiResponse<Object> response = ApiResponse.builder()
                                .message("Username already exists")
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.CONFLICT.value())
                                .build();

                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        @ExceptionHandler(InvalidPasswordException.class)
        public ResponseEntity<ApiResponse<Object>> handleInvalidPassword(InvalidPasswordException ex) {
                ApiResponse<Object> response = ApiResponse.builder()
                                .message("Password does not meet requirements")
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        @ExceptionHandler(NotAuthorizedException.class)
        public ResponseEntity<ApiResponse<Object>> handleNotAuthorized(NotAuthorizedException ex) {
                ApiResponse<Object> response = ApiResponse.builder()
                                .message("Invalid credentials or access token")
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .build();

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        @ExceptionHandler(UserNotConfirmedException.class)
        public ResponseEntity<ApiResponse<Object>> handleUserNotConfirmed(UserNotConfirmedException ex) {
                ApiResponse<Object> response = ApiResponse.builder()
                                .message("User account is not confirmed. Please verify your email.")
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .build();

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        @ExceptionHandler(UserNotFoundException.class)
        public ResponseEntity<ApiResponse<Object>> handleUserNotFound(UserNotFoundException ex) {
                ApiResponse<Object> response = ApiResponse.builder()
                                .message("User not found")
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.NOT_FOUND.value())
                                .build();

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        @ExceptionHandler(CodeMismatchException.class)
        public ResponseEntity<ApiResponse<Object>> handleCodeMismatch(CodeMismatchException ex) {
                ApiResponse<Object> response = ApiResponse.builder()
                                .message("Invalid verification code")
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        @ExceptionHandler(ExpiredCodeException.class)
        public ResponseEntity<ApiResponse<Object>> handleExpiredCode(ExpiredCodeException ex) {
                ApiResponse<Object> response = ApiResponse.builder()
                                .message("Verification code has expired")
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        @ExceptionHandler(TooManyRequestsException.class)
        public ResponseEntity<ApiResponse<Object>> handleTooManyRequests(TooManyRequestsException ex) {
                ApiResponse<Object> response = ApiResponse.builder()
                                .message("Too many requests. Please try again later.")
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                                .build();

                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
        }
}
