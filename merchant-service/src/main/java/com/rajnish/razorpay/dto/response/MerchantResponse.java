package com.rajnish.razorpay.dto.response;


import com.rajnish.razorpay.enums.BusinessType;
import com.rajnish.razorpay.enums.MerchantStatus;

import java.util.UUID;

public record MerchantResponse(
        UUID id,
        String name,
        String email,
        String businessName,
        BusinessType businessType,
        MerchantStatus merchantStatus
) {
}
