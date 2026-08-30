package com.rajnish.razorpay.services.impl;


import com.rajnish.razorpay.api.MerchantLookupService;
import com.rajnish.razorpay.dto.SettlementBankDetails;
import com.rajnish.razorpay.dto.WebhookTarget;
import com.rajnish.razorpay.entity.Merchant;
import com.rajnish.razorpay.enums.MerchantStatus;
import com.rajnish.razorpay.exceptions.ResourceNotFoundException;
import com.rajnish.razorpay.repository.MerchantRepository;
import com.rajnish.razorpay.repository.WebhookConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MerchantLookupServiceImpl implements MerchantLookupService {
    private final WebhookConfigRepository webhookConfigRepository;
    private final MerchantRepository merchantRepository;
    private final BytesEncryptor  bytesEncryptor;

    @Override
    public List<WebhookTarget> getActiveConfigForEvent(UUID merchantId, String eventType) {
        return webhookConfigRepository.findByMerchant_IdAndEnabledTrue(merchantId).stream()
                .filter(config->config.isSubscribedTo(eventType))
                .map(config ->{
                    byte[] cipherTextBytes = Base64.getDecoder().decode(config.getWebhookSecret());
                    byte[] decryptedSecretBytes = bytesEncryptor.decrypt(cipherTextBytes);
                    return new WebhookTarget(config.getId(), config.getTargetUrl(),
                            new String(decryptedSecretBytes, StandardCharsets.UTF_8));
                })
                .toList();

    }

    @Override
    public List<UUID> listActiveMerchantIds() {
        return merchantRepository.findByStatus(MerchantStatus.ACTIVE)
                .stream().map(m -> m.getId()).toList();
    }

    @Override
    public SettlementBankDetails getSettlementBankDetails(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId).orElseThrow(
                ()-> new ResourceNotFoundException("Merchant",  merchantId));

        return new  SettlementBankDetails(
                merchant.getSettlementBankAccount(),
                merchant.getSettlementBankIFSC(),
                merchant.getSettlementBankAccountHolderName()
        );
    }
}
