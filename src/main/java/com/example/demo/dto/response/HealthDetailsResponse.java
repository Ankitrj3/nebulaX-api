package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Detailed health information response object.
 * Provides system-level metrics and resource availability information.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.dto.response.HealthResponse
 * @see com.example.demo.controller.HealthController
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthDetailsResponse {

    /** Disk space availability status */
    private String diskSpace;

    /** Memory availability status */
    private String memory;

    /** Application uptime in milliseconds */
    private Long uptime;

    /** Error message if health check failed */
    private String error;
}
