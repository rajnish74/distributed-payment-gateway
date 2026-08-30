package com.rajnish.razorpay.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.util.UUID;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SettlementPaymentId {

    private UUID settlementId;

    private UUID paymentId;
}
