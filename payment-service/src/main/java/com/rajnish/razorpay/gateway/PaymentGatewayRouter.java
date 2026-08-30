package com.rajnish.razorpay.gateway;


import com.rajnish.razorpay.enums.PaymentMethod;
import com.rajnish.razorpay.gateway.dto.PaymentRequest;
import com.rajnish.razorpay.gateway.dto.PaymentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentGatewayRouter {

    private final Map<PaymentMethod, PaymentAdapter> paymentAdapterMap;

    public PaymentResult initiate(PaymentRequest request){
        PaymentAdapter adapter=paymentAdapterMap.get(request.method());
        if(adapter==null){
            throw new IllegalArgumentException("No adapter found for payment method: "+request.method());
        }
        return adapter.initiate(request);

    }

    public PaymentResult capture(PaymentMethod method, UUID paymentId) {
        PaymentAdapter adapter=paymentAdapterMap.get(method);
        if(adapter==null){
            throw new IllegalArgumentException("No adapter found for payment method: "+method);
        }
        return adapter.capture(paymentId);

    }
}
