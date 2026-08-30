package com.rajnish.razorpay.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rajnish.razorpay.entity.Money;
import com.rajnish.razorpay.enums.PaymentMethod;
import com.rajnish.razorpay.enums.PaymentStatus;


import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentResponse(
        UUID id,
        UUID orderId,
        UUID merchantId,
        Money amount,
        PaymentStatus status,
        PaymentMethod method,
        Map<String, Object> methodDetails,
        String errorCode,
        String errorDescriptions,
        LocalDateTime capturedAt,
        LocalDateTime createdAt
) {
}
