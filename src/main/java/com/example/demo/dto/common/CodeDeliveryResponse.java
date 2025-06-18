package com.example.demo.dto.common;

import lombok.Builder;
import lombok.Data;

/**
 * Response data for code delivery operations (confirmation codes, password
 * reset codes).
 * Provides standardized information about where and how codes are delivered.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.service.impl.CognitoServiceImpl#sendCodeWithRateLimit
 * @see com.example.demo.controller.AuthController#forgotPassword
 * @see com.example.demo.controller.AuthController#resendConfirmationCode
 */
@Data
@Builder
public class CodeDeliveryResponse {

    /** Masked destination where the code was sent (e.g., "u***@example.com") */
    private String destination;

    /** Delivery medium used (e.g., "EMAIL", "SMS") */
    private String deliveryMedium;

    /**
     * Attribute name associated with the delivery (e.g., "email", "phone_number")
     */
    private String attributeName;

    /**
     * Type of code being sent - "CONFIRMATION" for signup verification,
     * "PASSWORD_RESET" for password recovery
     */
    private String codeType;
}
