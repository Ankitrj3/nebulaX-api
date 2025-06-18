package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response object for validation error information.
 * Contains field-specific validation error messages for form validation
 * failures.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.exception.GlobalExceptionHandler
 * @see org.springframework.web.bind.MethodArgumentNotValidException
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {

    /** Map of field names to their corresponding validation error messages */
    private Map<String, String> fieldErrors;
}
