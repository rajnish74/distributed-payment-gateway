package com.rajnish.razorpay.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "card_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardToken  extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false,length = 50,unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "vault_card_id",nullable = false)
    private VaultCard vaultCard;

    private UUID customer;

    @Column(nullable = false)
    private UUID merchant;

    private LocalDateTime revokedAt;



}
