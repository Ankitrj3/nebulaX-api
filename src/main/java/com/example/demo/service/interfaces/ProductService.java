package com.example.demo.service.interfaces;

import com.example.demo.model.DataItem;
import java.math.BigDecimal;

/**
 * Service interface for product operations.
 * 
 * @author Vikas Singh
 * @since June 20, 2025
 */
public interface ProductService {

    /**
     * Create a new product
     */
    DataItem createProduct(String prodname, BigDecimal price);

    /**
     * Get a product by ID
     */
    DataItem getProduct(String id);
}
