package com.rajnish.razorpay.repository;

import com.rajnish.razorpay.entity.Settlement;
import com.rajnish.razorpay.enums.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {
    List<Settlement> findByStatus(SettlementStatus settlementStatus);
}
