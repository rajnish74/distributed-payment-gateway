package com.rajnish.razorpay.settlement;

import com.rajnish.razorpay.client.MerchantServiceClient;
import com.rajnish.razorpay.client.PaymentServiceClient;
import com.rajnish.razorpay.dto.PaymentSettlementView;
import com.rajnish.razorpay.dto.SettlementBankDetails;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SettlementIntegrationGateway {

    private final MerchantServiceClient  merchantServiceClient;
    private final PaymentServiceClient paymentServiceClient;

    @CircuitBreaker(name = "payment-service")
    @Retry(name = "payment-service ")
    public List<PaymentSettlementView> findUnSettledCaptured(UUID merchantId){
        return paymentServiceClient.findUnSettledCaptured(merchantId);
    }

    @CircuitBreaker(name = "payment-service")
    @Retry(name = "payment-service ")
    public void markSettled(List<UUID> paymentIds){
        paymentServiceClient.markSettled(paymentIds);
    }

    @CircuitBreaker(name = "merchant-service")
    @Retry(name = "merchant-service ")
    public SettlementBankDetails getSettlementBankDetails(UUID merchantId){
        return merchantServiceClient.getSettlementBankDetails(merchantId);
    }
}
