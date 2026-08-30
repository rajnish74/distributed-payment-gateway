package com.rajnish.razorpay.controller;

import com.rajnish.razorpay.context.MerchantContext;
import com.rajnish.razorpay.dto.request.CreateApiKeyRequest;
import com.rajnish.razorpay.dto.response.ApiKeyCreateResponse;
import com.rajnish.razorpay.dto.response.ApiKeyResponse;

import com.rajnish.razorpay.services.ApiKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("v1/merchants/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final MerchantContext merchantContext;

    @PostMapping
    public ResponseEntity<ApiKeyCreateResponse> createApiKey(@Valid
                                                             @RequestBody CreateApiKeyRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiKeyService.createApiKey(merchantContext.getMerchantId(), request));
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> getListMerchantApiKeys() {
        return ResponseEntity.ok(apiKeyService.getListMerchantApiKeys(merchantContext.getMerchantId()));
    }

    @DeleteMapping("/{keyId}")
    public ResponseEntity<Void> deleteApiKey(@PathVariable UUID keyId) {
        apiKeyService.deleteApiKey(merchantContext.getMerchantId(),keyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{keyId}/rotate")
    public ResponseEntity<ApiKeyCreateResponse> rotateApiKey(@PathVariable UUID keyId) {
        return ResponseEntity.ok(apiKeyService.rotateApiKey(merchantContext.getMerchantId(),keyId));
    }

}
