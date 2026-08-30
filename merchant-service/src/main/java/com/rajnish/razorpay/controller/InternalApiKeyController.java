package com.rajnish.razorpay.controller;

import com.rajnish.razorpay.cache.ApiKeyCacheEntry;
import com.rajnish.razorpay.entity.ApiKey;
import com.rajnish.razorpay.exceptions.ResourceNotFoundException;
import com.rajnish.razorpay.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/api-keys")
public class InternalApiKeyController {

    private final ApiKeyRepository apiKeyRepository;

    @GetMapping("/{keyId}")
    public ApiKeyCacheEntry findById(@PathVariable String keyId) {
        ApiKey apiKey = apiKeyRepository.findByKeyId(keyId)
                .orElseThrow(()-> new ResourceNotFoundException("ApiKey", keyId));

        return new ApiKeyCacheEntry(
                apiKey.getKeyId(),
                apiKey.getKeySecretHash(),
                apiKey.getPreviousKeySecretHash(),
                apiKey.getGracePeriodExpiresAt(),
                apiKey.getMerchant().getId(),
                apiKey.getEnvironment(),
                apiKey.isEnabled()
        );

    }
}
