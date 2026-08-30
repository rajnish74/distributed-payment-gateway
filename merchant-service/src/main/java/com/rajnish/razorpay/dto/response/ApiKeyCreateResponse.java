package com.rajnish.razorpay.dto.response;



import com.rajnish.razorpay.enums.Environment;

import java.util.UUID;

public record ApiKeyCreateResponse(
        UUID id,
        String keyId,
        String keySecret,
        Environment environment

) {
}
