package com.rajnish.razorpay.entity;


import com.rajnish.razorpay.enums.Environment;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "api_key",
    indexes = {
        @Index(name = "idx_api_key_key_merchant_env",columnList = "merchant_id, environment, enabled")
    })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiKey extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "merchant_id",nullable = false)
    private Merchant merchant;

    @Column(length = 100,nullable = false)
    private String keyId;

    @Column(length = 200,nullable = false)
    private String keySecretHash;

    @Column(length = 200)
    private String previousKeySecretHash;

    @Enumerated(EnumType.STRING)
    private Environment environment;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled=true;

    private LocalDateTime lastUsedAt;
    private LocalDateTime rotatedAt;
    private LocalDateTime gracePeriodExpiresAt;


    public boolean isInGracePeriod(){
        return gracePeriodExpiresAt != null && LocalDateTime.now().isBefore(gracePeriodExpiresAt);
    }

}
