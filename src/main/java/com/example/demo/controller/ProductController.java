package com.example.demo.controller;

import com.example.demo.dto.request.CreateDataItemRequest;
import com.example.demo.dto.common.ApiResponse;
import com.example.demo.model.DataItem;
import com.example.demo.service.interfaces.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for product operations.
 * Provides endpoints for creating and retrieving products.
 * 
 * @author Vikas Singh
 * @since June 20, 2025
 */
@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Create a new product
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DataItem>> createProduct(@RequestBody CreateDataItemRequest request) {
        try {
            log.info("Creating product with name: {}", request.getProdname());

            DataItem product = productService.createProduct(request.getProdname(), request.getPrice());

            return ResponseEntity.ok(ApiResponse.<DataItem>builder()
                    .success(true)
                    .status(200)
                    .message("Product created successfully")
                    .data(product)
                    .build());

        } catch (Exception e) {
            log.error("Error creating product: {}", e.getMessage(), e);

            return ResponseEntity.status(500).body(ApiResponse.<DataItem>builder()
                    .success(false)
                    .status(500)
                    .message("Failed to create product: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Get a product by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DataItem>> getProduct(@PathVariable String id) {
        try {
            log.info("Getting product with ID: {}", id);

            DataItem product = productService.getProduct(id);

            if (product == null) {
                return ResponseEntity.status(404).body(ApiResponse.<DataItem>builder()
                        .success(false)
                        .status(404)
                        .message("Product not found")
                        .build());
            }

            return ResponseEntity.ok(ApiResponse.<DataItem>builder()
                    .success(true)
                    .status(200)
                    .message("Product retrieved successfully")
                    .data(product)
                    .build());

        } catch (Exception e) {
            log.error("Error getting product: {}", e.getMessage(), e);

            return ResponseEntity.status(500).body(ApiResponse.<DataItem>builder()
                    .success(false)
                    .status(500)
                    .message("Failed to get product: " + e.getMessage())
                    .build());
        }
    }
}
