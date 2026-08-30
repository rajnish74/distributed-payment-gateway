package com.rajnish.razorpay.api;



import com.rajnish.razorpay.dto.PaymentSettlementView;
import com.rajnish.razorpay.entity.Payments;

import java.util.List;
import java.util.UUID;

public interface PaymentLookupService {

    List<PaymentSettlementView> findUnsettledCapturedPayments(UUID merchantId);

    void markSettled(List<UUID> paymentList);
}
