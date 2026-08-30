package com.rajnish.razorpay.dto;

import com.rajnish.razorpay.entity.Money;

import java.util.Map;
import java.util.UUID;

public record VaultChargeRequest(
        UUID paymentId,
        String token,
        Money amount,
        Map<String, Object> methodDetails
) {
}
