package com.rajnish.razorpay.dto;

public record SettlementBankDetails(
        String accountNumber,
        String ifsc,
        String accountHolderName
) {
}
