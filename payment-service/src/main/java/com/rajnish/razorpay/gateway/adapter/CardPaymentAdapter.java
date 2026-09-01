package com.rajnish.razorpay.gateway.adapter;


import com.rajnish.razorpay.client.VaultServiceClient;
import com.rajnish.razorpay.dto.PaymentProcessorResponse;
import com.rajnish.razorpay.dto.VaultChargeRequest;
import com.rajnish.razorpay.gateway.PaymentAdapter;
import com.rajnish.razorpay.gateway.dto.PaymentRequest;
import com.rajnish.razorpay.gateway.dto.PaymentResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;
@Component
@RequiredArgsConstructor
public class CardPaymentAdapter implements PaymentAdapter {

    private final VaultServiceClient  vaultServiceClient;

    @Override
    @CircuitBreaker(name = "vault-service")
    @Retry(name = "vault-service ")
    public PaymentResult initiate(PaymentRequest request){
        String token= (String) request.methodDetails().get("token");

        PaymentProcessorResponse response= vaultServiceClient.charge(
                new VaultChargeRequest(request.paymentId(), token, request.amount(), request.methodDetails())

        );

        return switch (response){
            case PaymentProcessorResponse.Success success->
                    new PaymentResult.Success(success.bankReference());
            case PaymentProcessorResponse.Pending pending->
                    new PaymentResult.Pending(pending.processorReference());
            case PaymentProcessorResponse.Failure failure->
                    new PaymentResult.Failure(failure.errorCode(),failure.errorDescription());
        };
    }

    @Override
    public PaymentResult capture(UUID paymentId) {
        return new PaymentResult.Success("CARD_REFERENCE");
    }
}
