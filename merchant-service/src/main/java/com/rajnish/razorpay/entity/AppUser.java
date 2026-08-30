package com.rajnish.razorpay.entity;


import com.rajnish.razorpay.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "app_user",
    indexes = {
        @Index(name = "idx_app_user_merchant_id",columnList = "merchant_id")
    })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AppUser  extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @Column(nullable = false,unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private UserRole role;



}
