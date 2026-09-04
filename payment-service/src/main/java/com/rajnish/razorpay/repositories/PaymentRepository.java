package com.rajnish.razorpay.repositories;


import com.rajnish.razorpay.entity.OrderRecord;
import com.rajnish.razorpay.entity.Payments;
import com.rajnish.razorpay.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payments, UUID> {
    List<Payments> findByOrder_Id(OrderRecord order);

    Optional<Payments> findByIdAndMerchantId(UUID paymentId, UUID merchantId);

    List<Payments> findByStatusAndCreatedAtBefore(PaymentStatus paymentStatus, LocalDateTime globalWindow);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payments p where p.id = :paymentId and p.merchantId = :merchantId ")
    Optional<Payments> findByIdAndMerchantIdForUpdate(UUID paymentId, UUID merchantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payments p where p.id = :paymentId ")
    Optional<Payments> findByIdForUpdate(UUID paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payments p where p.merchantId = :merchantId  and p.status = :paymentStatus and p.settledAt is null")
    List<Payments> findByMerchantIdAndStatusForUpdate(UUID merchantId, PaymentStatus paymentStatus);

    Optional<Payments> findByMerchantIdAndIdempotencyKey(UUID merchantId, String idempotencyKey);
}
