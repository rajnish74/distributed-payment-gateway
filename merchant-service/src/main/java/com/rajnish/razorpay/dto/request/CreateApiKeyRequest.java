package com.rajnish.razorpay.dto.request;


import com.rajnish.razorpay.enums.Environment;

public record CreateApiKeyRequest(
        Environment environment
) {
}
