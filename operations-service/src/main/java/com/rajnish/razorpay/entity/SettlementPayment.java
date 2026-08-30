package com.rajnish.razorpay.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "settlement_payments")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SettlementPayment extends BaseEntity {

    @EmbeddedId
    private SettlementPaymentId id;

    @MapsId("settlementId")
    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "settlement_id",nullable = false)
    private Settlement settlement;


}
