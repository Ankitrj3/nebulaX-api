package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Detailed error response object for comprehensive error information.
 * Provides detailed debugging information for development and support purposes.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.exception.GlobalExceptionHandler
 * @see com.example.demo.dto.response.ErrorResponse
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDetailsResponse {

    /** Exception class name that caused the error */
    private String exception;

    /** Root cause of the exception */
    private String cause;

    /** HTTP method that triggered the error */
    private String method;

    /** Array of supported HTTP methods for the endpoint */
    private String[] supportedMethods;

    /** Name of the parameter that caused validation error */
    private String parameterName;

    /** Type of the parameter that failed validation */
    private String parameterType;

    /** Field name that failed validation */
    private String field;

    /** Detailed error message */
    private String message;
}
