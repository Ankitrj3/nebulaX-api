package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response object for application health check operations.
 * Provides comprehensive health status information including uptime and system
 * details.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.controller.HealthController
 * @see com.example.demo.dto.response.HealthDetailsResponse
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthResponse {

    /** Overall health status of the application (UP/DOWN) */
    private String status;

    /** Descriptive message about the health check result */
    private String message;

    /** Timestamp when the health check was performed */
    private LocalDateTime timestamp;

    /** Detailed health information including system metrics */
    private HealthDetailsResponse details;

    /** Application uptime in seconds */
    private Long uptime;
    private String version;
}
