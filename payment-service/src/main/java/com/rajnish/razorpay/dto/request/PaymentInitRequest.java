package com.rajnish.razorpay.dto.request;


import com.rajnish.razorpay.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record PaymentInitRequest(

        @NotNull(message = "Order id is required")
        UUID orderId,

        @NotNull(message = "Payment method is required")
        PaymentMethod method,

        Map<String,Object> methodDetails
) {
}
