package com.rajnish.razorpay.services.impl;

import com.rajnish.razorpay.cache.ApiKeyCache;
import com.rajnish.razorpay.dto.request.CreateApiKeyRequest;
import com.rajnish.razorpay.dto.response.ApiKeyCreateResponse;
import com.rajnish.razorpay.dto.response.ApiKeyResponse;
import com.rajnish.razorpay.entity.ApiKey;
import com.rajnish.razorpay.entity.Merchant;
import com.rajnish.razorpay.exceptions.ResourceNotFoundException;
import com.rajnish.razorpay.mapper.ApiKeyMapper;
import com.rajnish.razorpay.repository.ApiKeyRepository;
import com.rajnish.razorpay.repository.MerchantRepository;
import com.rajnish.razorpay.services.ApiKeyService;
import com.rajnish.razorpay.utils.RandomizerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ApiKeyServiceImpl implements ApiKeyService {

    private final MerchantRepository merchantRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyMapper apiKeyMapper;
    private BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();
    private final ApiKeyCache apiKeyCache;

    @Override
    @Transactional
    public ApiKeyCreateResponse createApiKey(UUID merchantId, CreateApiKeyRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(()->new ResourceNotFoundException("merchant", merchantId));

        String keyId="rzp_"+request.environment().name().toLowerCase()+"_"+ RandomizerUtil.randomBase64(24);
        String rawSecret= RandomizerUtil.randomBase64(40);

        ApiKey apiKey= ApiKey.builder()
                .merchant(merchant)
                .keyId(keyId)
                .keySecretHash(BCRYPT.encode(rawSecret))
                .environment(request.environment())
                .build();

        apiKey=apiKeyRepository.save(apiKey);

        return new ApiKeyCreateResponse(
                apiKey.getId(),
                keyId,
                rawSecret,
                request.environment()
        );
    }

    @Override
    public List<ApiKeyResponse> getListMerchantApiKeys(UUID merchantId) {
        return apiKeyMapper.toResponseList(apiKeyRepository.findByMerchant_Id(merchantId));
    }

    @Override
    @Transactional
    public void deleteApiKey(UUID merchantId, UUID keyId) {
        ApiKey apiKey=apiKeyRepository.findById(keyId)
                .filter(k->k.getMerchant().getId().equals(merchantId))
                .orElseThrow(()->new ResourceNotFoundException("apiKey",keyId));

       apiKey.setEnabled(false);
       apiKeyCache.evict(apiKey.getKeyId());

    }

    @Override
    @Transactional
    public ApiKeyCreateResponse rotateApiKey(UUID merchantId, UUID keyId) {

        ApiKey apiKey=apiKeyRepository.findById(keyId)
                .filter(k->k.getMerchant().getId().equals(merchantId))
                .orElseThrow(()->new ResourceNotFoundException("apiKey",keyId));

        if(!apiKey.isEnabled()) throw new RuntimeException("Cannot rotate a disabled api key");

        String newRawSecret=RandomizerUtil.randomBase64(40);
        apiKey.setPreviousKeySecretHash(apiKey.getKeySecretHash());
        apiKey.setKeySecretHash(BCRYPT.encode(newRawSecret));
        apiKey.setRotatedAt(LocalDateTime.now());
        apiKey.setGracePeriodExpiresAt(LocalDateTime.now().plusHours(24));

        apiKey=apiKeyRepository.save(apiKey);

        apiKeyCache.evict(apiKey.getKeyId());

        return new ApiKeyCreateResponse(
                apiKey.getId(),
                apiKey.getKeyId(),
                newRawSecret,
                apiKey.getEnvironment()
        );
    }
}
