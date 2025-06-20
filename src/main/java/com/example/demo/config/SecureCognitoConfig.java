package com.example.demo.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

/**
 * Secure configuration for AWS Cognito integration using Parameter Store.
 * This class fetches sensitive Cognito configuration from AWS Systems Manager
 * Parameter Store instead of storing them directly in environment variables.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 */
@Slf4j
@Data
@Configuration
public class SecureCognitoConfig {

    @Value("${SSM_PARAM_PREFIX:/spring-boot-demo/cognito}")
    private String parameterPrefix;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    // Local profile configuration from properties file
    @Value("${aws.cognito.client-id:}")
    private String localClientId;

    @Value("${aws.cognito.client-secret:}")
    private String localClientSecret;

    @Value("${aws.cognito.user-pool-id:}")
    private String localUserPoolId;

    @Value("${aws.cognito.region:}")
    private String localRegion;

    @Bean
    public SsmClient ssmClient() {
        // Try to get the region from the environment first, fallback to us-east-1
        String region = System.getenv("AWS_REGION");
        if (region == null || region.isEmpty()) {
            region = System.getProperty("aws.region", "us-east-1");
        }
        
        return SsmClient.builder()
                .region(Region.of(region))
                .build();
    }

    @Bean
    public CognitoProperties cognitoProperties(SsmClient ssmClient) {
        // Check if running in local profile
        boolean isLocalProfile = activeProfiles.contains("local") || activeProfiles.isEmpty();

        if (isLocalProfile) {
            log.info("Local profile detected, attempting to load from environment variables first");
            try {
                CognitoProperties envProperties = createEnvironmentProperties();
                if (envProperties != null) {
                    log.info("Successfully loaded Cognito configuration from environment variables");
                    return envProperties;
                }
            } catch (Exception e) {
                log.warn("Failed to load from environment variables, falling back to Parameter Store: {}",
                        e.getMessage());
            }
        }

        log.info("Loading Cognito configuration from Parameter Store with prefix: {}", parameterPrefix);

        try {
            CognitoProperties properties = new CognitoProperties();

            // Fetch client ID (regular String)
            String clientId = getParameter(ssmClient, parameterPrefix + "/client-id");
            properties.setClientId(clientId);
            log.info("Successfully loaded Cognito Client ID from Parameter Store");

            // Fetch client secret (SecureString)
            String clientSecret = getSecureParameter(ssmClient, parameterPrefix + "/client-secret");
            properties.setClientSecret(clientSecret);
            log.info("Successfully loaded Cognito Client Secret from Parameter Store");

            // Fetch user pool ID (regular String)
            String userPoolId = getParameter(ssmClient, parameterPrefix + "/user-pool-id");
            properties.setUserPoolId(userPoolId);
            log.info("Successfully loaded Cognito User Pool ID from Parameter Store");

            // Fetch region (regular String)
            String region = getParameter(ssmClient, parameterPrefix + "/region");
            properties.setRegion(region);
            log.info("Successfully loaded Cognito Region from Parameter Store: {}", region);

            log.info("All Cognito configuration loaded successfully from Parameter Store");
            return properties;

        } catch (Exception e) {
            log.error("Failed to load Cognito configuration from Parameter Store", e);

            // Final fallback to environment variables
            log.warn("Falling back to environment variables for Cognito configuration");
            return createFallbackProperties();
        }
    }

    private String getParameter(SsmClient ssmClient, String parameterName) {
        try {
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(parameterName)
                    .build();

            GetParameterResponse response = ssmClient.getParameter(request);
            return response.parameter().value();
        } catch (Exception e) {
            log.error("Failed to get parameter: {}", parameterName, e);
            throw new RuntimeException("Failed to load parameter: " + parameterName, e);
        }
    }

    private String getSecureParameter(SsmClient ssmClient, String parameterName) {
        try {
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(parameterName)
                    .withDecryption(true) // Important: decrypt SecureString parameters
                    .build();

            GetParameterResponse response = ssmClient.getParameter(request);
            return response.parameter().value();
        } catch (Exception e) {
            log.error("Failed to get secure parameter: {}", parameterName, e);
            throw new RuntimeException("Failed to load secure parameter: " + parameterName, e);
        }
    }

    /**
     * Create CognitoProperties from environment variables or properties file
     * for local development.
     */
    private CognitoProperties createEnvironmentProperties() {
        String clientId = System.getenv("AWS_COGNITO_CLIENT_ID");
        String clientSecret = System.getenv("AWS_COGNITO_CLIENT_SECRET");
        String userPoolId = System.getenv("AWS_COGNITO_USER_POOL_ID");
        String region = System.getenv("AWS_COGNITO_REGION");

        // If environment variables are not set, try properties file values (for local
        // profile)
        if (clientId == null || clientSecret == null || userPoolId == null || region == null) {
            log.info("Environment variables not found, checking properties file for local profile");

            if (localClientId != null && !localClientId.isEmpty() &&
                    localClientSecret != null && !localClientSecret.isEmpty() &&
                    localUserPoolId != null && !localUserPoolId.isEmpty() &&
                    localRegion != null && !localRegion.isEmpty()) {

                clientId = localClientId;
                clientSecret = localClientSecret;
                userPoolId = localUserPoolId;
                region = localRegion;

                log.info("Successfully loaded Cognito configuration from properties file");
            }
        }

        // Check if all required configuration values are present
        if (clientId == null || clientSecret == null || userPoolId == null || region == null ||
                clientId.isEmpty() || clientSecret.isEmpty() || userPoolId.isEmpty() || region.isEmpty()) {
            log.warn("Missing required Cognito configuration");
            return null;
        }

        CognitoProperties properties = new CognitoProperties();
        properties.setClientId(clientId);
        properties.setClientSecret(clientSecret);
        properties.setUserPoolId(userPoolId);
        properties.setRegion(region);

        log.info("Loaded Cognito configuration successfully");
        log.info("Client ID: {}", clientId);
        log.info("User Pool ID: {}", userPoolId);
        log.info("Region: {}", region);
        log.info("Client Secret: [HIDDEN]");

        return properties;
    }

    /**
     * Fallback method to create CognitoProperties from environment variables
     * for local development when Parameter Store is not available.
     */
    private CognitoProperties createFallbackProperties() {
        CognitoProperties properties = createEnvironmentProperties();

        if (properties == null) {
            String errorMsg = "Missing required Cognito configuration. Please ensure Parameter Store is configured " +
                    "or set environment variables: AWS_COGNITO_CLIENT_ID, AWS_COGNITO_CLIENT_SECRET, " +
                    "AWS_COGNITO_USER_POOL_ID, AWS_COGNITO_REGION";
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        log.info("Using fallback Cognito configuration from environment variables");
        return properties;
    }
}
