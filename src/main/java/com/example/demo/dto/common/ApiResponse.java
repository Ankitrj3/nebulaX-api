package com.example.demo.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard API response wrapper for all REST endpoints.
 * Provides consistent response structure across the application.
 * 
 * @param <T> The type of data being returned
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.controller.AuthController
 * @see com.example.demo.service.interfaces.CognitoService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /** Human-readable message describing the response */
    private String message;

    /** Timestamp when the response was generated */
    private LocalDateTime timestamp;

    /** HTTP status code */
    private int status;

    /** Whether the operation was successful */
    private boolean success;

    /** The actual response data */
    private T data;

    /**
     * Creates a successful response with data.
     * 
     * @param <T>     Type of response data
     * @param message Success message
     * @param data    Response data
     * @return ApiResponse with success status and data
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .message(message)
                .timestamp(LocalDateTime.now())
                .status(200)
                .success(true)
                .data(data)
                .build();
    }

    /**
     * Creates a successful response without data.
     * 
     * @param <T>     Type of response data
     * @param message Success message
     * @return ApiResponse with success status
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .message(message)
                .timestamp(LocalDateTime.now())
                .status(200)
                .success(true)
                .build();
    }

    /**
     * Creates an error response.
     * 
     * @param <T>     Type of response data
     * @param message Error message
     * @param status  HTTP status code
     * @return ApiResponse with error status
     */
    public static <T> ApiResponse<T> error(String message, int status) {
        return ApiResponse.<T>builder()
                .message(message)
                .timestamp(LocalDateTime.now())
                .status(status)
                .success(false)
                .build();
    }

    public static <T> ApiResponse<T> error(String message, int status, T data) {
        return ApiResponse.<T>builder()
                .message(message)
                .timestamp(LocalDateTime.now())
                .status(status)
                .success(false)
                .data(data)
                .build();
    }
}
