package com.rajnish.razorpay.services;



import com.rajnish.razorpay.dto.request.UpdateWebhookConfigRequest;
import com.rajnish.razorpay.dto.response.WebhookConfigResponse;

import java.util.List;
import java.util.UUID;

public interface WebhookConfigService {

    WebhookConfigResponse create(UUID merchantId, UpdateWebhookConfigRequest request);

    List<WebhookConfigResponse> list(UUID merchantId);

    WebhookConfigResponse getById(UUID merchantId, UUID webhookConfigId);

    WebhookConfigResponse update(UUID merchantId, UUID webhookConfigId, UpdateWebhookConfigRequest request);

    void delete(UUID merchantId, UUID webhookConfigId);
}
