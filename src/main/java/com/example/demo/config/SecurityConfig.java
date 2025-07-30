package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Security configuration for the authentication application.
 * Configures Spring Security with CORS support, CSRF protection, and endpoint
 * access rules.
 * 
 * Features:
 * - CORS integration with CorsConfig
 * - Stateless session management for JWT tokens
 * - Public access to authentication endpoints
 * - Security headers configuration
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.config.CorsConfig
 * @see org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Autowired
        private CorsConfigurationSource corsConfigurationSource;

        /**
         * Configures the security filter chain with CORS, CSRF, and authorization
         * rules.
         * 
         * @param http HttpSecurity object to configure
         * @return SecurityFilterChain with the configured security settings
         * @throws Exception if configuration fails
         */
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                // Enable CORS with custom configuration
                                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                                // Disable CSRF for stateless API
                                .csrf(csrf -> csrf.disable())

                                // Stateless session management for JWT tokens
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // Configure endpoint access rules
                                .authorizeHttpRequests(authz -> authz
                                                .requestMatchers("/api/auth/**").permitAll() // Public authentication
                                                                                             // endpoints
                                                .requestMatchers("/api/products/**").permitAll() // Public product
                                                                                                 // endpoints for
                                                                                                 // testing
                                                .requestMatchers("/api/health").permitAll() // Health check endpoint
                                                .requestMatchers("/health").permitAll() // Alternative health endpoint
                                                .requestMatchers("/actuator/**").permitAll() // Spring Boot Actuator
                                                                                             // endpoints
                                                .anyRequest().authenticated() // All other endpoints require
                                                                              // authentication
                                )

                                // Configure security headers
                                .headers(headers -> headers
                                                .frameOptions(frameOptions -> frameOptions.deny()) // Prevent
                                                                                                   // clickjacking
                                                .contentTypeOptions(contentTypeOptions -> {
                                                }) // Prevent MIME type sniffing
                                                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                                                                .maxAgeInSeconds(31536000) // HSTS for 1 year
                                                                .includeSubDomains(true) // Include subdomains
                                                ));

                return http.build();
        }
}
