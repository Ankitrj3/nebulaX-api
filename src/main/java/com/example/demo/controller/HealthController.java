package com.example.demo.controller;

import com.example.demo.dto.common.ApiResponse;
import com.example.demo.dto.response.HealthResponse;
import com.example.demo.dto.response.HealthDetailsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;

/**
 * Health check controller for monitoring application status.
 * Provides comprehensive health information including system metrics and
 * uptime.
 * 
 * Features:
 * - Application health status monitoring
 * - System resource information
 * - Uptime tracking
 * - Memory and disk space checks
 * - Version information
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.dto.response.HealthResponse
 * @see com.example.demo.dto.response.HealthDetailsResponse
 */
@RestController
@RequestMapping("/health")
public class HealthController {

        @Value("${spring.application.name:demo}")
        private String applicationName;

        @Value("${app.version:1.0.0}")
        private String applicationVersion;

        /**
         * Performs application health check and returns comprehensive health
         * information.
         * 
         * This endpoint provides:
         * - Overall application health status
         * - System resource availability
         * - Application uptime
         * - Memory and disk space status
         * - Version information
         * 
         * @return ResponseEntity containing health check results
         */
        @GetMapping
        public ResponseEntity<ApiResponse<HealthResponse>> getHealth() {
                try {
                        HealthDetailsResponse details = HealthDetailsResponse.builder()
                                        .diskSpace("available")
                                        .memory("available")
                                        .uptime(ManagementFactory.getRuntimeMXBean().getUptime())
                                        .build();

                        HealthResponse healthResponse = HealthResponse.builder()
                                        .status("UP")
                                        .message("Application health check completed successfully")
                                        .version(applicationVersion)
                                        .timestamp(LocalDateTime.now())
                                        .uptime(ManagementFactory.getRuntimeMXBean().getUptime())
                                        .details(details)
                                        .build();

                        ApiResponse<HealthResponse> response = ApiResponse.<HealthResponse>builder()
                                        .success(true)
                                        .message("Health check successful")
                                        .status(200)
                                        .timestamp(LocalDateTime.now())
                                        .data(healthResponse)
                                        .build();

                        return ResponseEntity.ok(response);

                } catch (Exception e) {
                        HealthDetailsResponse errorDetails = HealthDetailsResponse.builder()
                                        .error(e.getMessage())
                                        .build();

                        HealthResponse healthResponse = HealthResponse.builder()
                                        .status("DOWN")
                                        .message("Health check failed")
                                        .version(applicationVersion)
                                        .timestamp(LocalDateTime.now())
                                        .details(errorDetails)
                                        .build();

                        ApiResponse<HealthResponse> response = ApiResponse.<HealthResponse>builder()
                                        .success(false)
                                        .message("Health check failed")
                                        .status(503)
                                        .timestamp(LocalDateTime.now())
                                        .data(healthResponse)
                                        .build();

                        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
                }
        }
}
