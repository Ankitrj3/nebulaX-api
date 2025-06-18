package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

/**
 * AWS Cognito configuration class for setting up Cognito Identity Provider
 * client.
 * Configures the AWS SDK client with proper region and credentials for Cognito
 * operations.
 * 
 * Features:
 * - AWS SDK v2 client configuration
 * - Default credential provider chain
 * - Region-specific client setup
 * - Automatic credential resolution
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.config.CognitoProperties
 * @see software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
 */
@Configuration
public class CognitoConfig {

    @Autowired
    private CognitoProperties cognitoProperties;

    /**
     * Creates and configures AWS Cognito Identity Provider client.
     * 
     * @return CognitoIdentityProviderClient configured with region and credentials
     */
    @Bean
    public CognitoIdentityProviderClient cognitoIdentityProviderClient() {
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(cognitoProperties.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
