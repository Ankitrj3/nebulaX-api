package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response data object for user registration (signup) operations.
 * Contains user information and code delivery details after successful
 * registration.
 * 
 * @author Vikas Singh
 * @since June 18, 2025
 * @see com.example.demo.controller.AuthController#signUp
 * @see com.example.demo.service.interfaces.CognitoService#signUp
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignUpResponseData {

    /** User's unique identifier (sub) from Cognito */
    private String userSub;

    /** Username chosen during registration */
    private String username;

    /** Details about how the confirmation code was delivered */
    private CodeDeliveryDetails codeDeliveryDetails;

    /**
     * Inner class for code delivery information.
     * Contains details about how verification codes are sent to users.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeDeliveryDetails {

        /** Masked destination where the code was sent (e.g., j***@example.com) */
        private String destination;

        /** Delivery medium used (EMAIL, SMS) */
        private String deliveryMedium;

        /** Attribute name that received the code (email, phone_number) */
        private String attributeName;
    }
}
