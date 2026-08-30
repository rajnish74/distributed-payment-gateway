package com.rajnish.razorpay.outbox;


import com.rajnish.razorpay.entity.OutboxEvent;
import com.rajnish.razorpay.enums.OutboxStatus;

import com.rajnish.razorpay.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OutboxResultHandler {

    private final OutboxEventRepository outboxEventRepository;
    private final Integer MAX_ATTEMPTS = 3;

    @Transactional
    public void handleEventPublished(OutboxEvent event) {
        event.setStatus(OutboxStatus.PUBLISHED);
        event.setPublishedAt(LocalDateTime.now());
        outboxEventRepository.save(event);
    }

    public void handleEventFailed(OutboxEvent event, String message) {
        event.setAttempts(event.getAttempts() + 1);
        event.setLastError(
                message.length() < 1000 ? message : message.substring(0, 1000));

        if (event.getAttempts() >= MAX_ATTEMPTS) {
            event.setStatus(OutboxStatus.FAILED);
        }
        outboxEventRepository.save(event);
    }
}
