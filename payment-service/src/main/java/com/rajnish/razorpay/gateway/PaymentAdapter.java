package com.rajnish.razorpay.gateway;



import com.rajnish.razorpay.gateway.dto.PaymentRequest;
import com.rajnish.razorpay.gateway.dto.PaymentResult;

import java.util.UUID;

public interface PaymentAdapter {

    PaymentResult initiate(PaymentRequest request);

    PaymentResult capture(UUID paymentId);
}
