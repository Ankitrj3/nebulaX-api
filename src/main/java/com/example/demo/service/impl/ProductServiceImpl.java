package com.example.demo.service.impl;

import com.example.demo.model.DataItem;
import com.example.demo.service.interfaces.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service implementation for product operations using DynamoDB.
 * 
 * @author Vikas Singh
 * @since June 20, 2025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    @Override
    public DataItem createProduct(String prodname, java.math.BigDecimal price) {
        String id = UUID.randomUUID().toString();

        DataItem product = DataItem.builder()
                .id(id)
                .prodname(prodname)
                .price(price)
                .build();

        try {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("id", AttributeValue.builder().s(id).build());
            item.put("prodname", AttributeValue.builder().s(prodname).build());
            item.put("price", AttributeValue.builder().n(price.toString()).build());

            PutItemRequest request = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build();

            dynamoDbClient.putItem(request);

            log.info("Successfully created product with ID: {}", id);
            return product;

        } catch (Exception e) {
            log.error("Error creating product: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create product", e);
        }
    }

    @Override
    public DataItem getProduct(String id) {
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("id", AttributeValue.builder().s(id).build());

            GetItemRequest request = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(request);

            if (!response.hasItem()) {
                return null;
            }

            Map<String, AttributeValue> item = response.item();
            return DataItem.builder()
                    .id(item.get("id").s())
                    .prodname(item.get("prodname").s())
                    .price(new java.math.BigDecimal(item.get("price").n()))
                    .build();

        } catch (Exception e) {
            log.error("Error getting product with ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to get product", e);
        }
    }
}
