package com.rajnish.razorpay.repository;


import com.rajnish.razorpay.entity.WebhookEvent;
import com.rajnish.razorpay.enums.WebhookEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
    List<WebhookEvent> findByStatusAndNextRetryAtBefore(WebhookEventStatus webhookEventStatus, LocalDateTime now);
    
}
