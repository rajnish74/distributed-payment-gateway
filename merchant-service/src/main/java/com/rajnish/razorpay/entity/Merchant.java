package com.rajnish.razorpay.entity;


import com.rajnish.razorpay.enums.BusinessType;
import com.rajnish.razorpay.enums.MerchantStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "merchants",
    indexes = {
        @Index(name="idx_merchant_status",columnList = "status")
    })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Merchant  extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false,length = 200)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private BusinessType businessType;

    @Column(length = 100)
    private String businessName;

    @Column(length = 200)
    private String websiteURL;

    @Column(length = 200,nullable = false)
    @Enumerated(EnumType.STRING)
    private MerchantStatus status=MerchantStatus.PENDING_KYC;

    @Column(length = 50)
    private String gstId;

    @Column(length = 50)
    private String panId;

    @Column(length = 200)
    private String settlementBankAccount;

    @Column(length = 11)
    private String settlementBankIFSC;

    @Column(length = 200)
    private String settlementBankAccountHolderName;
}
