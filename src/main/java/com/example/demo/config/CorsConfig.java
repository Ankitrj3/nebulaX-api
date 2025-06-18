package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Cross-Origin Resource Sharing (CORS) configuration for the application.
 * Configures allowed origins, methods, and headers for cross-origin requests.
 * 
 * Security Features:
 * - Environment-specific origin configuration
 * - Restricted HTTP methods
 * - Controlled header exposure
 * - Credential handling configuration
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see org.springframework.web.cors.CorsConfiguration
 * @see com.example.demo.config.SecurityConfig
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Value("${app.cors.allowed-methods}")
    private List<String> allowedMethods;

    @Value("${app.cors.allowed-headers}")
    private List<String> allowedHeaders;

    @Value("${app.cors.exposed-headers}")
    private List<String> exposedHeaders;

    @Value("${app.cors.allow-credentials}")
    private boolean allowCredentials;

    @Value("${app.cors.max-age}")
    private long maxAge;

    /**
     * Creates and configures CORS configuration source.
     * 
     * @return CorsConfigurationSource with application-specific CORS settings
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Set allowed origins from configuration
        configuration.setAllowedOrigins(allowedOrigins);

        // Set allowed HTTP methods
        configuration.setAllowedMethods(allowedMethods);

        // Set allowed headers
        configuration.setAllowedHeaders(allowedHeaders);

        // Set exposed headers (headers that the client can access)
        configuration.setExposedHeaders(exposedHeaders);

        // Allow cookies and authentication headers
        configuration.setAllowCredentials(allowCredentials);

        // Cache preflight response for specified time
        configuration.setMaxAge(maxAge);

        // Apply CORS configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
