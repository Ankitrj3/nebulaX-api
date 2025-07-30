package com.example.demo.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new product.
 * 
 * @author Vikas Singh
 * @since June 20, 2025
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDataItemRequest {

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
