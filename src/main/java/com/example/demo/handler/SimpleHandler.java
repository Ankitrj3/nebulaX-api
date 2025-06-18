package com.example.demo.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple Lambda handler for testing basic functionality without Spring Boot overhead.
 */
public class SimpleHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        context.getLogger().log("Received request: " + input.getPath());
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        
        String body;
        int statusCode = 200;
        
        if (input.getPath().equals("/health")) {
            body = "{\"status\":\"UP\",\"message\":\"Simple handler working\",\"timestamp\":\"" + 
                   System.currentTimeMillis() + "\"}";
        } else {
            body = "{\"status\":\"OK\",\"message\":\"Simple handler response\",\"path\":\"" + 
                   input.getPath() + "\"}";
        }
        
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(headers)
                .withBody(body);
    }
}
