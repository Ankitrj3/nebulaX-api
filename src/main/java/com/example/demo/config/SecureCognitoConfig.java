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

    @Bean
    public SsmClient ssmClient() {
        return SsmClient.builder()
                .region(Region.US_EAST_1) // Default region, can be made configurable
                .build();
    }

    @Bean
    public CognitoProperties cognitoProperties(SsmClient ssmClient) {
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
            
            // Fallback to environment variables for local development
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
     * Fallback method to create CognitoProperties from environment variables
     * for local development when Parameter Store is not available.
     */
    private CognitoProperties createFallbackProperties() {
        CognitoProperties properties = new CognitoProperties();
        
        properties.setClientId(System.getenv("AWS_COGNITO_CLIENT_ID"));
        properties.setClientSecret(System.getenv("AWS_COGNITO_CLIENT_SECRET"));
        properties.setUserPoolId(System.getenv("AWS_COGNITO_USER_POOL_ID"));
        properties.setRegion(System.getenv("AWS_COGNITO_REGION"));
        
        // Validate that all required properties are set
        if (properties.getClientId() == null || properties.getClientSecret() == null ||
            properties.getUserPoolId() == null || properties.getRegion() == null) {
            
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
