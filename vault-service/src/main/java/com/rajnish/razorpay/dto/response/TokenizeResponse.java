package com.rajnish.razorpay.dto.response;


import com.rajnish.razorpay.enums.CardBrand;

public record TokenizeResponse(
        String token,
        String lastFour,
        CardBrand brand,
        Integer expiryMonth,
        Integer expiryYear
) {
}
