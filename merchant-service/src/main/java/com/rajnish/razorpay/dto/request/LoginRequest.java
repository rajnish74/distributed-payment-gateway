package com.rajnish.razorpay.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email
        String email,

        @NotBlank
        String password
) {
}
