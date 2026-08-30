package com.rajnish.razorpay.entity;


import com.rajnish.razorpay.enums.PaymentMethod;
import com.rajnish.razorpay.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "payments",
    indexes = {
        @Index(name ="idx_payment_order_id", columnList = "order_id"),
            @Index(name = "idx_payment_merchant_id", columnList = "merchant_id")
    })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Payments  extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "order_id",nullable = false)
    private OrderRecord order;

    @Column(nullable = false)
    private UUID merchantId;

    @Embedded
    private Money  amount;

    @Column(nullable = false,length = 100)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 50)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 50)
    private PaymentMethod method;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb",name = "method_details")
    private Map<String,Object> methodDetails;

    @Column(length = 100)
    private String bankReference;

    @Column(length = 100)
    private String processorReference;

    @Column(length = 100)
    private String errorCode;

    @Column(length = 200)
    private String errorMessage;

    private LocalDateTime authorizedAt;

    private LocalDateTime capturedAt;

    private LocalDateTime failedAt;

    private LocalDateTime refundedAt;

    private LocalDateTime settledAt;




}
