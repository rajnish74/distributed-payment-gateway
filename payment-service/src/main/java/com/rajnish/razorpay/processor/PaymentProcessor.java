package com.rajnish.razorpay.processor;


import com.rajnish.razorpay.dto.PaymentProcessorResponse;
import com.rajnish.razorpay.dto.PaymentProcessorRequest;

public interface PaymentProcessor {

    PaymentProcessorResponse charge(PaymentProcessorRequest request);
}
