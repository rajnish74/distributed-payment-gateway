package com.rajnish.razorpay.api;


import com.rajnish.razorpay.dto.SettlementBankDetails;
import com.rajnish.razorpay.dto.WebhookTarget;

import java.util.List;
import java.util.UUID;

public interface MerchantLookupService {

    List<WebhookTarget> getActiveConfigForEvent(UUID merchantId, String eventType);

    List<UUID> listActiveMerchantIds();

    SettlementBankDetails getSettlementBankDetails(UUID merchantId);
}
