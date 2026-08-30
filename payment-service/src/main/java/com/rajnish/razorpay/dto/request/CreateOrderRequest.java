package com.rajnish.razorpay.dto.request;


import com.rajnish.razorpay.entity.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Map;

public record CreateOrderRequest(

        @NotNull(message = "Amount is required")
        Money amount,

        @Size(max = 100, message = "Receipt should not exceed 100 characters")
        String receipt,
        Map<String, Object> notes,
        LocalDateTime expiresAt,

        @Valid
        CustomerDetails customer
) {

        public record CustomerDetails(
                @Size(max = 200)
                String name,

                @Email
                @Size(max = 200)
                String email,

                @Size(max = 200)
                String phone
        ){}
}
