package com.rajnish.razorpay.settlement;


import com.rajnish.razorpay.entity.Money;
import com.rajnish.razorpay.settlement.dto.BankTransferResult;

import java.util.UUID;

public interface BankTransferProcessor {

    BankTransferResult initiate(UUID settlementId, UUID merchantId,
                                Money amount, String bankAccount, String ifsc);
}
