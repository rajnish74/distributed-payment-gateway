package com.rajnish.razorpay.repository;


import com.rajnish.razorpay.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByMerchant_IdAndEmail(UUID merchantId, String email);
}
