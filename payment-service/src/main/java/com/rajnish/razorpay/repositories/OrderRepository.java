package com.rajnish.razorpay.repositories;


import com.rajnish.razorpay.entity.OrderRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderRecord, UUID> {
    boolean existsByMerchantIdAndReceipt(UUID merchantId, String receipt);

   Optional<OrderRecord> findByIdAndMerchantId(UUID merchantId, UUID orderId);

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT o FROM OrderRecord o where o.id = :uuid and o.merchantId = :merchantId ")
    Optional<OrderRecord> findByIdAndMerchantIdForUpdate(UUID uuid, UUID merchantId);
}
