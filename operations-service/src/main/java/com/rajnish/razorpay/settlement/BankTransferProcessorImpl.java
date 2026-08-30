package com.rajnish.razorpay.settlement;


import com.rajnish.razorpay.entity.Money;
import com.rajnish.razorpay.settlement.dto.BankTransferResult;
import com.rajnish.razorpay.utils.RandomizerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
@Component
@Slf4j
@RequiredArgsConstructor
public class BankTransferProcessorImpl implements BankTransferProcessor {
    @Override
    public BankTransferResult initiate(UUID settlementId, UUID merchantId, Money amount,
                                       String bankAccount, String ifsc) {

        // Call the Bank API

        String registrationRef = "TXN_"+ RandomizerUtil.randomBase64(12);


        log.debug("Bank Transfer call completed for settlementId: {}, registrationRef: {}",
                settlementId, registrationRef);

        return new BankTransferResult(registrationRef);
    }
}
