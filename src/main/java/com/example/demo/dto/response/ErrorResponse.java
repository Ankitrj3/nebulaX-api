package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response object for error information in API responses.
 * Provides standardized error details for debugging and user feedback.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.controller.CustomErrorController
 * @see com.example.demo.exception.GlobalExceptionHandler
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /** HTTP status code of the error */
    private Integer status;

    /** Error type or category */
    private String error;

    /** Human-readable error message */
    private String message;

    /** Request path where the error occurred */
    private String path;

    /** Timestamp when the error occurred */
    private String timestamp;
}
