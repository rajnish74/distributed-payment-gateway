package com.rajnish.razorpay.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WebhookConfigResponse(
        UUID id,
        String targetUrl,
        String webhookSecret,
        boolean enabled,
        String eventType
) {

}
