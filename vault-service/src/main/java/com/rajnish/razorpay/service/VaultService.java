package com.rajnish.razorpay.service;



import com.rajnish.razorpay.dto.PaymentProcessorResponse;
import com.rajnish.razorpay.dto.request.TokenizeRequest;
import com.rajnish.razorpay.dto.response.TokenizeResponse;
import com.rajnish.razorpay.entity.Money;

import java.util.Map;
import java.util.UUID;

public interface VaultService {
    TokenizeResponse tokenize(TokenizeRequest request, UUID merchantId);

    PaymentProcessorResponse charge(UUID paymentId, String token, Money amount, Map<String, Object> methodDetails);
}
