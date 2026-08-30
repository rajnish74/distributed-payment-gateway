package com.rajnish.razorpay.services;



import com.rajnish.razorpay.dto.request.CreateApiKeyRequest;
import com.rajnish.razorpay.dto.response.ApiKeyCreateResponse;
import com.rajnish.razorpay.dto.response.ApiKeyResponse;

import java.util.List;
import java.util.UUID;

public interface ApiKeyService {
    ApiKeyCreateResponse createApiKey(UUID merchantId, CreateApiKeyRequest request);

    List<ApiKeyResponse> getListMerchantApiKeys(UUID merchantId);

    void deleteApiKey(UUID merchantId, UUID keyId);

    ApiKeyCreateResponse rotateApiKey(UUID merchantId, UUID keyId);
}
