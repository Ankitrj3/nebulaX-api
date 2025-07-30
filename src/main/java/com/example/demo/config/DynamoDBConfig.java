package com.example.demo.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

/**
 * DynamoDB configuration for Spring Boot application.
 * Configures DynamoDB client and retrieves table name from Parameter Store.
 * 
 * @author Vikas Singh
 * @since June 20, 2025
 */
@Slf4j
@Data
@Configuration
public class DynamoDBConfig {

    @Value("${SSM_PARAM_PREFIX:/spring-boot-demo/dynamodb}")
    private String parameterPrefix;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    @Value("${aws.ssm.enabled:true}")
    private boolean ssmEnabled;

    @Value("${aws.dynamodb.table-name:spring-boot-demo-products-dev}")
    private String localTableName;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        // Try to get the region from the environment first, fallback to us-east-1
        String region = System.getenv("AWS_REGION");
        if (region == null || region.isEmpty()) {
            region = System.getProperty("aws.region", "us-east-1");
        }

        return DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public String dynamoDbTableName(SsmClient ssmClient) {
        log.info("Loading DynamoDB table name with SSM enabled: {}, active profiles: {}", ssmEnabled, activeProfiles);

        // For local development, skip Parameter Store and use configured table name
        if (!ssmEnabled || "local".equals(activeProfiles)) {
            log.info("Using local DynamoDB table name: {}", localTableName);
            return localTableName;
        }

        // For Lambda environment, load from Parameter Store
        log.info("Loading DynamoDB table name from Parameter Store with prefix: {}", parameterPrefix);

        try {
            String tableName = getParameter(ssmClient, parameterPrefix + "/table-name");
            log.info("Successfully loaded DynamoDB table name: {}", tableName);
            return tableName;
        } catch (Exception e) {
            log.error("Failed to load DynamoDB table name from Parameter Store", e);

            // Fallback to environment variable
            String fallbackTableName = System.getenv("DYNAMODB_TABLE_NAME");
            if (fallbackTableName != null && !fallbackTableName.isEmpty()) {
                log.info("Using fallback DynamoDB table name from environment: {}", fallbackTableName);
                return fallbackTableName;
            }

            String errorMsg = "Missing DynamoDB table name configuration. Please ensure Parameter Store is configured "
                    +
                    "or set environment variable: DYNAMODB_TABLE_NAME";
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
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
}
