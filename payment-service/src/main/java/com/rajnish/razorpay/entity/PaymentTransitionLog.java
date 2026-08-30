package com.rajnish.razorpay.entity;

import com.rajnish.razorpay.enums.PaymentActor;
import com.rajnish.razorpay.enums.PaymentEvent;
import com.rajnish.razorpay.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_transition_log",
    indexes = {
        @Index(name = "idx_payment_transition_log_payment_id",columnList = "payment_id")
    })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentTransitionLog  extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "payment_id",nullable = false)
    private Payments payments;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status",length = 50)
    private PaymentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_event",nullable = false,length = 50)
    private PaymentEvent event;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status",nullable = false,length = 50)
    private PaymentStatus toStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor",length = 100)
    private PaymentActor actor;

    @Column(name = "occurred_at",nullable = false)
    private LocalDateTime occurredAt;
}
