package com.rajnish.razorpay.outbox;

import com.rajnish.razorpay.entity.OutboxEvent;
import com.rajnish.razorpay.enums.EventAggregateType;
import com.rajnish.razorpay.repositories.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;

    public void publish(EventAggregateType aggregateType, UUID aggregateId, String eventType,
                        Map<String, Object> payload) {
        OutboxEvent outboxevent = OutboxEvent.builder()
                .aggregateId(aggregateId)
                .aggregateType(aggregateType)
                .eventType(eventType)
                .payload(payload)
                .build();

        outboxEventRepository.save(outboxevent);
    }
}
