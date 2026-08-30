package com.rajnish.razorpay.controller;

import com.rajnish.razorpay.api.PaymentLookupService;
import com.rajnish.razorpay.dto.PaymentSettlementView;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/payments")
public class InternalSettlementController {

    private final PaymentLookupService settlementLookupService;

    @GetMapping("/unsettled-captured")
    public List<PaymentSettlementView> findUnsettledCaptured(@RequestParam UUID merchantId){
        return settlementLookupService.findUnsettledCapturedPayments(merchantId);
    }

    @PostMapping("/mark-settled")
    public void markSettled(@RequestBody List<UUID> paymentIds){
        settlementLookupService.markSettled(paymentIds);
    }

}
