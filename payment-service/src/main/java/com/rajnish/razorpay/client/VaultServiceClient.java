package com.rajnish.razorpay.client;

import com.rajnish.razorpay.dto.PaymentProcessorResponse;
import com.rajnish.razorpay.dto.VaultChargeRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "vault-service", path = "/internal/vault")
public interface VaultServiceClient {

    @PostMapping("/charge")
    PaymentProcessorResponse charge(@RequestBody VaultChargeRequest request);
}
