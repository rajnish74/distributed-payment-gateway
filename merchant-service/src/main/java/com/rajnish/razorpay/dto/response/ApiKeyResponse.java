package com.rajnish.razorpay.dto.response;


import com.rajnish.razorpay.enums.Environment;

import java.time.LocalDateTime;
import java.util.UUID;

public record ApiKeyResponse(
        UUID id,
        String keyId,
        Environment environment,
        boolean enabled,
        LocalDateTime lastUsedAt,
        LocalDateTime createdAt
) {
}
