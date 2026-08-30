package com.rajnish.razorpay.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DLQEvent  extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    @OneToOne(fetch = FetchType.LAZY)
    private WebhookEvent webhookEvent;

    @Column(length = 10000)
    private String finalError;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false,columnDefinition = "jsonb")
    private Map<String, Object> payload;

    private LocalDateTime movedAt;

    private LocalDateTime replayedAt;
}
