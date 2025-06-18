package com.example.demo.config;

import lombok.Data;

/**
 * Configuration properties for AWS Cognito integration.
 * Contains AWS Cognito configuration values loaded securely from Parameter Store
 * or environment variables as fallback.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.config.SecureCognitoConfig
 */
@Data
public class CognitoProperties {

    /** AWS Cognito User Pool Client ID */
    private String clientId;

    /** AWS Cognito User Pool Client Secret */
    private String clientSecret;

    /** AWS Cognito User Pool ID */
    private String userPoolId;

    /** AWS Region where the Cognito User Pool is located */
    private String region;
}
