package com.rajnish.razorpay.repository;


import com.rajnish.razorpay.entity.MerchantWebhookConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookConfigRepository extends JpaRepository<MerchantWebhookConfig, UUID> {
    List<MerchantWebhookConfig> findByMerchant_Id(UUID merchantId);

    Optional<MerchantWebhookConfig> findByIdAndMerchant_Id(UUID webhookConfigId, UUID merchantId);

    List<MerchantWebhookConfig> findByMerchant_IdAndEnabledTrue(UUID merchantId);
}
