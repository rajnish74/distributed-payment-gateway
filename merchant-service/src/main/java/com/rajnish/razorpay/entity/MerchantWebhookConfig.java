package com.rajnish.razorpay.entity;


import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "merchant_webhook_config",
    indexes = {
        @Index(name = "idx_merchant_webhook_config_merchant_id",columnList = "merchant_id, enabled")
    })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MerchantWebhookConfig  extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(nullable = false,name = "merchant_id")
    private Merchant merchant;

    @Column(nullable = false,length = 500)
    private String targetUrl;  //www.zara.com/webhook/success

    @Column(length = 500)
    private String webhookSecret;

    @Column(nullable = false)
    private Boolean enabled=true;

    @Column(length = 200)
    private String eventType;   //cooma seperated list of event types to subscribe to


    public boolean isSubscribedTo(String eventType) {

        if (this.eventType == null || this.eventType.isBlank()) {
            return true;
        }

        for (String type : this.eventType.split(",")) {

            String trimmed = type.trim();

            if (trimmed.equalsIgnoreCase("ALL")
                    || trimmed.equalsIgnoreCase(eventType)) {
                return true;
            }
        }

        return false;
    }


}
