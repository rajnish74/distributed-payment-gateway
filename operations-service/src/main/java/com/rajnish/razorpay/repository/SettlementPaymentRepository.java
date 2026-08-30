package com.rajnish.razorpay.repository;


import com.rajnish.razorpay.entity.Settlement;
import com.rajnish.razorpay.entity.SettlementPayment;
import com.rajnish.razorpay.entity.SettlementPaymentId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementPaymentRepository extends JpaRepository<SettlementPayment, SettlementPaymentId> {
    List<SettlementPayment> findBySettlement(Settlement settlement);
}
