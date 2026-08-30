package com.rajnish.razorpay.service.impl;

import com.rajnish.razorpay.api.PaymentLookupService;
import com.rajnish.razorpay.dto.PaymentSettlementView;
import com.rajnish.razorpay.entity.Payments;
import com.rajnish.razorpay.enums.PaymentStatus;
import com.rajnish.razorpay.repositories.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentLookupServiceImpl implements PaymentLookupService {

    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public List<PaymentSettlementView> findUnsettledCapturedPayments(UUID merchantId) {
        List<Payments> paymentsList = paymentRepository
                .findByMerchantIdAndStatusForUpdate(merchantId, PaymentStatus.CAPTURED);

        return paymentsList.stream()
                .map(p-> new PaymentSettlementView(
                        p.getId(),
                        p.getAmount().getAmountUnits(),
                        0, // TODO: replace with actual refund repository
                        p.getAmount().getCurrency()))
                .toList();
    }

    @Override
    @Transactional
    public void markSettled(List<UUID> paymentList) {
        LocalDateTime now = LocalDateTime.now();
        List<Payments> payments = paymentRepository.findAllById(paymentList);
        for (Payments payment: payments) {
            payment.setStatus(PaymentStatus.CAPTURED);
            payment.setSettledAt(now);
        }
        paymentRepository.saveAll(payments);
    }
}
