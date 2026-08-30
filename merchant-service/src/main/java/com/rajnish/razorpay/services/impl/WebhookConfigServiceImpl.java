package com.rajnish.razorpay.services.impl;


import com.rajnish.razorpay.dto.request.UpdateWebhookConfigRequest;
import com.rajnish.razorpay.dto.response.WebhookConfigResponse;
import com.rajnish.razorpay.entity.Merchant;
import com.rajnish.razorpay.entity.MerchantWebhookConfig;
import com.rajnish.razorpay.exceptions.ResourceNotFoundException;
import com.rajnish.razorpay.mapper.WebhookConfigMapper;
import com.rajnish.razorpay.repository.MerchantRepository;
import com.rajnish.razorpay.repository.WebhookConfigRepository;
import com.rajnish.razorpay.services.WebhookConfigService;
import com.rajnish.razorpay.utils.RandomizerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookConfigServiceImpl implements WebhookConfigService {

    private final WebhookConfigRepository webhookConfigRepository;
    private final MerchantRepository merchantRepository;
    private final BytesEncryptor bytesEncryptor;
    private final WebhookConfigMapper webhookConfigMapper;

    @Override
    public WebhookConfigResponse create(UUID merchantId, UpdateWebhookConfigRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(()-> new ResourceNotFoundException("Merchant", merchantId));

        String rawSecret = RandomizerUtil.randomBase64(32);
        byte[] rawSecretBytes = rawSecret.getBytes(StandardCharsets.UTF_8);

        String encryptedSecret = Base64.getEncoder().encodeToString
                (bytesEncryptor.encrypt(rawSecretBytes));

        MerchantWebhookConfig config = MerchantWebhookConfig.builder()
                .merchant(merchant)
                .targetUrl(request.targetUrl())
                .enabled(true)
                .eventType(request.eventType())
                .webhookSecret(encryptedSecret)
                .build();

        config = webhookConfigRepository.save(config);

        return webhookConfigMapper.toResponse(config, rawSecret);
    }

    @Override
    public List<WebhookConfigResponse> list(UUID merchantId) {
        return webhookConfigRepository.findByMerchant_Id(merchantId).stream()
                .map(config-> webhookConfigMapper.toResponse(config, null))
                .toList();

    }

    @Override
    public WebhookConfigResponse getById(UUID merchantId, UUID webhookConfigId) {
        MerchantWebhookConfig config = reqireOwnedConfig(merchantId, webhookConfigId);
        return webhookConfigMapper.toResponse(config, null);
    }

    @Override
    public WebhookConfigResponse update(UUID merchantId, UUID webhookConfigId, UpdateWebhookConfigRequest request) {
        MerchantWebhookConfig config = reqireOwnedConfig(merchantId, webhookConfigId);
        config.setTargetUrl(request.targetUrl());
        config.setEventType(request.eventType());
        log.info("Updating webhook config with id {} merchantId {}", webhookConfigId, merchantId);
        return webhookConfigMapper.toResponse(config, null);
    }

    @Override
    public void delete(UUID merchantId, UUID webhookConfigId) {
        MerchantWebhookConfig config = reqireOwnedConfig(merchantId, webhookConfigId);
        webhookConfigRepository.delete(config);
        log.info("Deleting webhook config with id {} merchantId {}", webhookConfigId, merchantId);

    }

    private MerchantWebhookConfig reqireOwnedConfig(UUID merchantId, UUID webhookConfigId) {
        return webhookConfigRepository.findByIdAndMerchant_Id(webhookConfigId, merchantId)
                .orElseThrow(()-> new ResourceNotFoundException("WebhookConfig", webhookConfigId));
    }


}
