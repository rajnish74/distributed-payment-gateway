package com.rajnish.razorpay.settlement;


import com.rajnish.razorpay.client.MerchantServiceClient;
import com.rajnish.razorpay.client.PaymentServiceClient;
import com.rajnish.razorpay.dto.PaymentSettlementView;
import com.rajnish.razorpay.dto.SettlementBankDetails;
import com.rajnish.razorpay.entity.Money;
import com.rajnish.razorpay.entity.Settlement;
import com.rajnish.razorpay.entity.SettlementPayment;
import com.rajnish.razorpay.entity.SettlementPaymentId;
import com.rajnish.razorpay.enums.EventAggregateType;
import com.rajnish.razorpay.enums.SettlementStatus;
import com.rajnish.razorpay.exceptions.ResourceNotFoundException;
import com.rajnish.razorpay.outbox.OutboxEventPublisher;
import com.rajnish.razorpay.repository.SettlementPaymentRepository;
import com.rajnish.razorpay.repository.SettlementRepository;
import com.rajnish.razorpay.settlement.dto.BankTransferResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class SettlementTransactionExecutor {


    private final SettlementRepository settlementRepository;
    private final SettlementPaymentRepository settlementPaymentRepository;
    private final BankTransferProcessor bankTransferProcessor;
    private final OutboxEventPublisher outboxEventPublisher;
    private final PaymentServiceClient  paymentServiceClient;
    private final MerchantServiceClient  merchantServiceClient;
    private static final double FEE_RATE = 0.02;
    private static final double GST_RATE = 0.18;

    @Transactional
    public void processForMerchant(UUID merchantId, LocalDate settlementDate) {
        List<PaymentSettlementView> unsettledPayments = paymentServiceClient.findUnSettledCaptured(merchantId);
        if (unsettledPayments.isEmpty()) return;

        log.info("Processing {} unsettled payments for merchantId: {} on {} date",
                unsettledPayments.size(), merchantId, settlementDate);

        Integer grossAmount = unsettledPayments.stream()
                .map(PaymentSettlementView::amountUnits)
                .reduce(Integer::sum)
                .orElse(0);

        Money gross = Money.of(grossAmount, unsettledPayments.getFirst().currency());

        int fee = Math.toIntExact(Math.round(gross.getAmountUnits() * FEE_RATE));
        int gst = Math.toIntExact(Math.round(fee * GST_RATE));
        Money feeAmount = Money.of(fee, gross.getCurrency());
        Money gstAmount = Money.of(gst, gross.getCurrency());
        Money netAmount = gross.sub(feeAmount).sub(gstAmount);

        Settlement settlement = Settlement.builder()
                .merchantId(merchantId)
                .grossAmount(gross)
                .gstAmount(gstAmount)
                .netAmount(netAmount)
                .feeAmount(feeAmount)
                .status(SettlementStatus.INITIATED)
                .build();

        settlementRepository.save(settlement);

        try {
            List<SettlementPayment> links = new ArrayList<>();
            for (PaymentSettlementView p : unsettledPayments) {
                links.add(SettlementPayment.builder()
                        .id(new SettlementPaymentId(settlement.getId(), p.paymentId()))
                        .settlement(settlement)
                        .build());
            }

            settlementPaymentRepository.saveAll(links);


            SettlementBankDetails settlementBankDetails = merchantServiceClient.getSettlementBankDetails(merchantId);
            BankTransferResult bankTransferResult = bankTransferProcessor.initiate(
                    settlement.getId(),
                    merchantId,
                    netAmount,
                    settlementBankDetails.accountNumber(),
                    settlementBankDetails.ifsc()
            );

            settlement.setStatus(SettlementStatus.TRANSFER_PENDING);
            settlement.setBankReference(bankTransferResult.registrationRef());

            settlementRepository.save(settlement);
        } catch (Exception e) {
            log.error("Settlement failed for settlementId: {}, error: {}, on {} date", settlement.getId(),settlementDate,  e.getMessage());
            settlement.setStatus(SettlementStatus.FAILED);
            settlementRepository.save(settlement);
        }
    }

    public void resolveTransfer(UUID settlementId,
                                String errorCode, String errorDescription) {

        Settlement settlement = settlementRepository.findById(settlementId).orElseThrow(
                ()-> new ResourceNotFoundException("Settlement", settlementId));

        if(settlement.getStatus() != SettlementStatus.TRANSFER_PENDING) {
            log.info("Settlement resolved, skipping for id: {}", settlement.getId());
            return;
        }

        if (errorCode == null) { // success
            settlement.setStatus(SettlementStatus.SETTLED);
            settlement.setProcessedAt(LocalDateTime.now());
            settlementRepository.save(settlement);

            List<SettlementPayment> settlementPaymentList = settlementPaymentRepository.findBySettlement(settlement);
            List<UUID> paymentIds = settlementPaymentList.stream()
                            .map(SettlementPayment::getId)
                                    .map(SettlementPaymentId::getPaymentId)
                                            .toList();

            paymentServiceClient.markSettled(paymentIds);


            log.info("Settlement resolved, successfully settled for id: {}", settlement.getId());
            outboxEventPublisher.publish(EventAggregateType.SETTLEMENT, settlementId,
                    "SETTLEMENT_PROCESSED", Map.of(
                            "settlementId", settlement,
                            "merchantId", settlement.getMerchantId(),
                            "status", settlement.getStatus().name(),
                            "settlementAmount", settlement.getNetAmount().getAmountUnits(),
                            "settlementCurrency", settlement.getNetAmount().getCurrency()
                    ));
        } else { //failed
            settlement.setStatus(SettlementStatus.FAILED);
            settlement.setFailureReason(errorCode+" : "+errorDescription);
            settlementRepository.save(settlement);
            log.warn("Settlement failed for id: {}", settlement.getId());
            outboxEventPublisher.publish(EventAggregateType.SETTLEMENT, settlementId,
                    "SETTLEMENT_FAILED", Map.of(
                            "settlementId", settlement,
                            "merchantId", settlement.getMerchantId(),
                            "status", settlement.getStatus().name(),
                            "settlementAmount", settlement.getNetAmount().getAmountUnits(),
                            "settlementCurrency", settlement.getNetAmount().getCurrency()
                    ));

        }

    }
}
