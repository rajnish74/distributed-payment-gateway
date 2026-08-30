package com.rajnish.razorpay.gateway.dto;



import com.rajnish.razorpay.entity.Money;
import com.rajnish.razorpay.enums.PaymentMethod;

import java.util.Map;
import java.util.UUID;

public record PaymentRequest(
        UUID paymentId,
        UUID orderId,
        UUID merchantId,
        Money amount,
        PaymentMethod method,
        Map<String,Object> methodDetails
) {
}
