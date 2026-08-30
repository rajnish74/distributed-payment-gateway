package com.rajnish.razorpay.dto.request;


import com.rajnish.razorpay.enums.BusinessType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MerchantSignupRequest(

        @NotNull(message = "Name is required")
        @Size(max = 50,message = "Name should not exceed 50 characters")
        String name,

        @Email
        @NotNull(message = "Email is required")
        String email,

        @NotNull(message = "Password is required")
        @Size(min = 8, message = "Password should be at least 8 characters long")
        String password,

        @Size(max = 50,message = "Business name should not exceed 50 characters")
        String businessName,

        BusinessType businessType
) {
}
