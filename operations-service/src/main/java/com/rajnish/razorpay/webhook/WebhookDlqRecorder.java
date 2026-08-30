package com.rajnish.razorpay.webhook;

import com.rajnish.razorpay.entity.DLQEvent;
import com.rajnish.razorpay.entity.WebhookEvent;
import com.rajnish.razorpay.enums.WebhookEventStatus;
import com.rajnish.razorpay.repository.DlqEventRepository;
import com.rajnish.razorpay.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDlqRecorder {

    private final WebhookEventRepository webhookEventRepository;
    private final DlqEventRepository dlqEventRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAfterAttemptsExhausted(WebhookEvent webhookEvent, String finalError) {
        webhookEvent.setStatus(WebhookEventStatus.DEAD);
        webhookEventRepository.save(webhookEvent);

        DLQEvent dlqEvent = DLQEvent.builder()
                .webhookEvent(webhookEvent)
                .merchantId(webhookEvent.getMerchantId())
                .finalError(finalError)
                .payload(webhookEvent.getPayload())
                .build();
        dlqEventRepository.save(dlqEvent);
    }

    public void recordConsumerFailed(ConsumerRecord<String, Map<String, Object>> record, String error) {
        Map<String, Object> envelope = record.value();
        UUID merchantId = null;

        try{
            Map<String, Object> data = (Map<String, Object>) envelope.get("data");
            Object merchantIdRaw = data != null ? data.get("merchantId") : null;
            if(merchantIdRaw != null){
                merchantId = UUID.fromString(merchantIdRaw.toString());
            }
        } catch (Exception ignored) {

        }

        DLQEvent dlqEvent = DLQEvent.builder()
                .webhookEvent(null)
                .merchantId(merchantId)
                .finalError(error)
                .payload(envelope != null ? envelope: Map.of())
                .build();

        dlqEventRepository.save(dlqEvent);
    }
}
