package com.rajnish.razorpay.controller;


import com.rajnish.razorpay.context.MerchantContext;
import com.rajnish.razorpay.dto.request.UpdateWebhookConfigRequest;
import com.rajnish.razorpay.dto.response.WebhookConfigResponse;
import com.rajnish.razorpay.services.WebhookConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/merchants/webhooks")
public class WebhookConfigController {

    private final WebhookConfigService webhookConfigService;
    private final MerchantContext merchantContext;

    @PostMapping
    public ResponseEntity<WebhookConfigResponse> create(@Valid @RequestBody UpdateWebhookConfigRequest request) {
        return ResponseEntity.ok(webhookConfigService.create(merchantContext.getMerchantId(), request));
    }

    @GetMapping
    public ResponseEntity<List<WebhookConfigResponse>> getAll() {
        return ResponseEntity.ok(webhookConfigService.list(merchantContext.getMerchantId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebhookConfigResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(webhookConfigService.getById(merchantContext.getMerchantId(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebhookConfigResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateWebhookConfigRequest request) {
        return ResponseEntity.ok(webhookConfigService.update(merchantContext.getMerchantId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        webhookConfigService.delete(merchantContext.getMerchantId(), id);
        return ResponseEntity.noContent().build();
    }
}
