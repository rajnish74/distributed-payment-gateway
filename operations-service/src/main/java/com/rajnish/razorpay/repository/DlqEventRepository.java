package com.rajnish.razorpay.repository;


import com.rajnish.razorpay.entity.DLQEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DlqEventRepository extends JpaRepository<DLQEvent, UUID> {
}
