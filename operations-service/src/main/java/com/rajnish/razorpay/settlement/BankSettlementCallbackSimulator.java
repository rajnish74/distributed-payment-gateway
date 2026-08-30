package com.rajnish.razorpay.settlement;


import com.rajnish.razorpay.entity.Settlement;
import com.rajnish.razorpay.enums.SettlementStatus;
import com.rajnish.razorpay.repository.SettlementRepository;
import com.rajnish.razorpay.utils.RandomizerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class BankSettlementCallbackSimulator {

    private final SettlementRepository settlementRepository;
    private final SettlementTransactionExecutor  settlementTransactionExecutor;

    @Scheduled(fixedDelayString = "5000")
    public void processCallbacks(){
        List<Settlement> settlements = settlementRepository.findByStatus(SettlementStatus.TRANSFER_PENDING);
        if(settlements.isEmpty()) return;

        for(Settlement settlement : settlements){
            simulateCallback(settlement);
        }
    }

    private void simulateCallback(Settlement settlement){
        log.info("Initiating settlement callback for settlement with id: {}", settlement.getId());
        String utrNumber = "UTR_"+ RandomizerUtil.randomBase64(12);
        settlementTransactionExecutor.resolveTransfer(settlement.getId(),null, null);
    }
}
