package com.rajnish.razorpay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateWebhookConfigRequest(
        @NotBlank(message = "Webhook URL is required")
        @Size(max = 500)
        @Pattern(regexp = "^(https?://).+", message = "Webhook URL must start with http:// or https://")
        String targetUrl,

        @Size(max = 1000)
        String eventType
) {



}
