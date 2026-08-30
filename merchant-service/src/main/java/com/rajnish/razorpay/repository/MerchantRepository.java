package com.rajnish.razorpay.repository;


import com.rajnish.razorpay.entity.Merchant;
import com.rajnish.razorpay.enums.MerchantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
    boolean existsByEmail(String email);

    List<Merchant> findByStatus(MerchantStatus merchantStatus);
}
