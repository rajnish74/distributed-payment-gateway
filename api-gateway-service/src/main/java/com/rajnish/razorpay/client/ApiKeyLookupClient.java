package com.rajnish.razorpay.client;

import com.rajnish.razorpay.cache.ApiKeyCacheEntry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "merchant-service", path = "/internal/api-keys")
public interface ApiKeyLookupClient {

    @GetMapping("/{keyId}")
    ApiKeyCacheEntry findByKeyId(@PathVariable String keyId);
}
