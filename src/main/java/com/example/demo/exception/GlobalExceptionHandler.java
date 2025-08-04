package com.example.demo.exception;

import com.example.demo.dto.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;

/**
 * Global exception handler for the application.
 * Provides centralized exception handling across all controllers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles 404 Not Found exceptions when endpoints don't exist.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(NoHandlerFoundException ex, WebRequest request) {
        ApiResponse<Object> response = ApiResponse.builder()
                .message("Endpoint not found: " + ex.getRequestURL())
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handles general exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneral(Exception ex, WebRequest request) {
        ApiResponse<Object> response = ApiResponse.builder()
                .message("An error occurred: " + ex.getMessage())
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handles illegal argument exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        ApiResponse<Object> response = ApiResponse.builder()
                .message("Invalid request: " + ex.getMessage())
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .success(false)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}