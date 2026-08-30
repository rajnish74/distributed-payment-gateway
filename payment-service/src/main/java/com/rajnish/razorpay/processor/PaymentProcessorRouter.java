package com.rajnish.razorpay.processor;


import com.rajnish.razorpay.dto.PaymentProcessorResponse;
import com.rajnish.razorpay.enums.PaymentMethod;
import com.rajnish.razorpay.dto.PaymentProcessorRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PaymentProcessorRouter {

    private final Map<PaymentMethod,PaymentProcessor> paymentProcessors;

    public PaymentProcessorResponse charge(PaymentProcessorRequest request) {
        PaymentProcessor processor=paymentProcessors.get(request.method());
        if(processor==null){
            throw new IllegalArgumentException("No processor found for payment method: "+request.method());
        }
        return processor.charge(request);
    }
}
