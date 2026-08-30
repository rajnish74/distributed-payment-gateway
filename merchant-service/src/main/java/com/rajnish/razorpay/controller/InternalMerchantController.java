package com.rajnish.razorpay.controller;

import com.rajnish.razorpay.api.MerchantLookupService;
import com.rajnish.razorpay.dto.SettlementBankDetails;
import com.rajnish.razorpay.dto.WebhookTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RequestMapping("/internal/merchants")
@RestController
public class InternalMerchantController {

    private final MerchantLookupService  merchantLookupService;

    @GetMapping("/{merchantId}/webhook-targets")
    public List<WebhookTarget> getActiveConfigForEvent(@PathVariable UUID merchantId,
                                                       @RequestParam String eventTYpe) {

        return merchantLookupService.getActiveConfigForEvent(merchantId, eventTYpe);
    }

    @GetMapping("/active-ids")
    public List<UUID> listActiveMerchantIds() {
        return merchantLookupService.listActiveMerchantIds();
    }

    @GetMapping("/{merchantId}/settlement-bank-details")
    public SettlementBankDetails getSettlementBankDetails(@PathVariable UUID merchantId) {
        return merchantLookupService.getSettlementBankDetails(merchantId);
    }
}
