package com.example.demo.handler;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.example.demo.DemoApplication;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * AWS Lambda handler for running Spring Boot application in serverless
 * environment.
 * Provides serverless deployment capability using AWS Lambda with API Gateway
 * integration.
 * 
 * Features:
 * - Spring Boot container initialization
 * - AWS API Gateway proxy integration
 * - Request/response stream handling
 * - Cold start optimization
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.DemoApplication
 */
public class StreamLambdaHandler implements RequestStreamHandler {

    /** Spring Boot Lambda container handler for AWS proxy requests */
    private static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

    static {
        try {
            // Initialize Spring Boot application container for Lambda
            handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(DemoApplication.class);
        } catch (ContainerInitializationException e) {
            // If we fail here, re-throw the exception to force another cold start
            e.printStackTrace();
            throw new RuntimeException("Could not initialize Spring Boot application", e);
        }
    }

    /**
     * Handles incoming Lambda requests by proxying them through the Spring Boot
     * application.
     * 
     * @param inputStream  Input stream containing the Lambda request
     * @param outputStream Output stream for the Lambda response
     * @param context      Lambda execution context containing runtime information
     * @throws IOException if there's an error processing the request/response
     *                     streams
     */
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {
        // Proxy the request through the Spring Boot application
        handler.proxyStream(inputStream, outputStream, context);
    }
}
