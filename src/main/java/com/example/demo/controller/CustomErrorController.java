package com.example.demo.controller;

import com.example.demo.dto.common.ApiResponse;
import com.example.demo.dto.response.ErrorResponse;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Custom error controller for handling application errors in a standardized
 * way.
 * Replaces the default Spring Boot error handling with consistent API
 * responses.
 * 
 * Features:
 * - Standardized error response format
 * - HTTP status code mapping
 * - Error attribute extraction
 * - Security-conscious error messages
 * - Consistent timestamp format
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.dto.response.ErrorResponse
 * @see com.example.demo.dto.common.ApiResponse
 * @see org.springframework.boot.web.servlet.error.ErrorController
 */
@RestController
public class CustomErrorController implements ErrorController {

    private final ErrorAttributes errorAttributes;

    /**
     * Constructor for CustomErrorController.
     * 
     * @param errorAttributes Spring Boot error attributes for extracting error
     *                        information
     */
    public CustomErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    /**
     * Handles all application errors and returns standardized error responses.
     * 
     * This method processes various types of errors:
     * - 404 Not Found errors
     * - 500 Internal Server Errors
     * - Authentication and authorization errors
     * - Validation errors
     * - Custom application errors
     * 
     * @param request HttpServletRequest containing error information
     * @return ResponseEntity with standardized error response
     */
    @RequestMapping("/error")
    public ResponseEntity<ApiResponse<ErrorResponse>> handleError(HttpServletRequest request) {
        WebRequest webRequest = new ServletWebRequest(request);

        // Get error attributes with stack trace for debugging (you might want to
        // disable this in production)
        ErrorAttributeOptions options = ErrorAttributeOptions.of(
                ErrorAttributeOptions.Include.MESSAGE,
                ErrorAttributeOptions.Include.BINDING_ERRORS,
                ErrorAttributeOptions.Include.EXCEPTION);

        Map<String, Object> errorAttributes = this.errorAttributes.getErrorAttributes(webRequest, options);

        Integer status = (Integer) errorAttributes.get("status");
        String message = (String) errorAttributes.get("message");
        String error = (String) errorAttributes.get("error");
        String path = (String) errorAttributes.get("path");
        String timestamp = (String) errorAttributes.get("timestamp");

        // Create a more user-friendly message based on status code
        String userMessage = getUserFriendlyMessage(status, message, error);

        // Create structured error response
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(status)
                .error(error)
                .message(userMessage)
                .path(path)
                .timestamp(timestamp)
                .build();

        // Create response with proper error structure
        ApiResponse<ErrorResponse> response = ApiResponse.<ErrorResponse>builder()
                .message(userMessage)
                .timestamp(LocalDateTime.now())
                .status(status != null ? status : 500)
                .success(false)
                .data(errorResponse)
                .build();

        HttpStatus httpStatus = HttpStatus.valueOf(status != null ? status : 500);
        return ResponseEntity.status(httpStatus).body(response);
    }

    private String getUserFriendlyMessage(Integer status, String message, String error) {
        if (status == null)
            return "An unknown error occurred";

        return switch (status) {
            case 400 -> "Bad Request: The request was invalid or malformed";
            case 401 -> "Unauthorized: Authentication is required to access this resource";
            case 403 -> "Forbidden: You don't have permission to access this resource";
            case 404 -> "Not Found: The requested resource could not be found";
            case 405 -> "Method Not Allowed: The HTTP method is not supported for this endpoint";
            case 408 -> "Request Timeout: The request took too long to process";
            case 409 -> "Conflict: The request conflicts with the current state of the resource";
            case 429 -> "Too Many Requests: Rate limit exceeded, please try again later";
            case 500 -> "Internal Server Error: Something went wrong on our end";
            case 502 -> "Bad Gateway: Invalid response from upstream server";
            case 503 -> "Service Unavailable: The service is temporarily unavailable";
            case 504 -> "Gateway Timeout: The upstream server took too long to respond";
            default -> error != null ? error : "An error occurred while processing your request";
        };
    }
}
