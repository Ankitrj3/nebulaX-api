package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Product model representing an item in the DynamoDB table.
 * Simple product data with id, name, and price.
 * 
 * @author Vikas Singh
 * @since June 20, 2025
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataItem {

    /**
     * Unique identifier for the data item (Primary Key)
     */
    @JsonProperty("id")
    private String id;

    /**
     * Product name
     */
    @JsonProperty("prodname")
    private String prodname;

    /**
     * Product price
     */
    @JsonProperty("price")
    private BigDecimal price;
}
