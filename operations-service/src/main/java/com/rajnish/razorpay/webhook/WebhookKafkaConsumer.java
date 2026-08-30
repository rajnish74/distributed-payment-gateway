package com.rajnish.razorpay.webhook;


import com.rajnish.razorpay.client.MerchantServiceClient;
import com.rajnish.razorpay.dto.WebhookTarget;
import com.rajnish.razorpay.entity.WebhookEvent;
import com.rajnish.razorpay.enums.WebhookEventStatus;
import com.rajnish.razorpay.repository.WebhookEventRepository;
import com.rajnish.razorpay.utils.SignerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.CannotCreateTransactionException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookKafkaConsumer {

    private final MerchantServiceClient  merchantServiceClient;
    private final ObjectMapper objectMapper;
    private final SignerUtil signerUtil;
    private final WebhookEventRepository webhookEventRepository;
    private final WebhookRetryQueue retryQueue;
    private final WebhookDlqRecorder dlqRecorder;

    @KafkaListener(topics ={
            "${app.kafka.topics.payment:payments.events}",
            "${app.kafka.topics.order:orders.events}",
            "${app.kafka.topics.refund:refunds.events}",
            "${app.kafka.topics.settlement:settlement.events}"
    })
    public void onWebhookEvent(ConsumerRecord<String, Map<String, Object>> record, Acknowledgment acknowledgment) {
        // ===== DEBUG LOGS =====
        log.info("========== WEBHOOK CONSUMER HIT ==========");
        log.info("Envelope: {}", record.value());
        try {
            Map<String, Object> envelope = record.value();
            Map<String, Object> data = (Map<String, Object>) envelope.get("data");
            String eventType = (String) envelope.get("eventType");
            log.info("Event Type: {}", eventType);

            Object merchantIdRaw = data.get("merchantId");
            log.info("MerchantId Raw: {}", merchantIdRaw);
            if (merchantIdRaw == null) {
                log.warn("Merchant id is not found, skipping event: {}", eventType);
                acknowledgment.acknowledge();
                return;
            }

            UUID merchantId = UUID.fromString(merchantIdRaw.toString());

            List<WebhookTarget> targets = merchantServiceClient.getActiveConfigForEvent(merchantId, eventType);
            if (targets.isEmpty()) {
                log.debug("No webhook target is found, skipping event: {}", eventType);
                acknowledgment.acknowledge();
                return;
            }

            Map<String, Object> signatureData = Map.of(
                    "event", eventType,
                    "payload",
                    "data"
            );
            String signatureJson = objectMapper.writeValueAsString(signatureData);

            for (WebhookTarget target : targets) {
                String signature = signerUtil.sign(signatureJson, target.webhookSecret());

                WebhookEvent webhookEvent = WebhookEvent.builder()
                        .merchantId(merchantId)
                        .eventType(eventType)
                        .payload(data)
                        .targetUrl(target.targetUrl())
                        .signature(signature)
                        .status(WebhookEventStatus.PENDING)
                        .nextRetryAt(LocalDateTime.now())
                        .build();

                webhookEventRepository.save(webhookEvent);

                retryQueue.enqueue(webhookEvent.getId(), webhookEvent.getNextRetryAt());
                log.info("Webhook event sent to webhook queue: {}", webhookEvent.getId());

            }
            acknowledgment.acknowledge();
        } catch (DataAccessException | CannotCreateTransactionException dbDown) {
            log.error("Webhook consumer failed, Due to DB down, count not process the record, offset: {}", record.offset());
        } catch (Exception logicError) {
            dlqRecorder.recordConsumerFailed(record, logicError.getMessage());
            acknowledgment.acknowledge();
        }

    }
}
