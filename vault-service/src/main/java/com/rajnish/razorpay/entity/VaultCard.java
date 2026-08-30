package com.rajnish.razorpay.entity;


import com.rajnish.razorpay.enums.CardBrand;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vault_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaultCard  extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false,length = 4)
    private String lastFourDigits;

    @Column(nullable = false,length = 6)
    private String bin;

    @Column(nullable = false)
    private byte[] encryptedPan;

    @Column(nullable = false)
    private byte[] encryptedDek;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CardBrand brand;

    @Column(nullable = false)
    private String expiryMonth;

    @Column(nullable = false)
    private String expiryYear;

    @Column(nullable = false)
    private String cardholderName;

    private LocalDateTime deletedAt;


}
