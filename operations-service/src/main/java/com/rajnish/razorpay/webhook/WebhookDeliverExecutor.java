package com.rajnish.razorpay.webhook;


import com.rajnish.razorpay.entity.WebhookEvent;
import com.rajnish.razorpay.enums.WebhookEventStatus;
import com.rajnish.razorpay.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebhookDeliverExecutor {

    private final WebhookEventRepository webhookEventRepository;
    private final WebhookRetryQueue webhookRetryQueue;
    private final RestClient  restClient;
    private final WebhookDlqRecorder webhookDlqRecorder;

    private final int MAX_ATTEMPTS = 7;

    @Value("${webhook.delivery.signature-header:X-Razorpay-Signature}")
    private String signatureHeader;

    private static final List<Duration> BACKOFF = List.of(
            Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(30),
            Duration.ofHours(2), Duration.ofHours(8), Duration.ofHours(24)
    );


    @Transactional
    public void deliver(UUID webhookEventId){
        Optional<WebhookEvent> webhookEvent = webhookEventRepository.findById(webhookEventId);
        if(webhookEvent.isEmpty()){
            log.warn("Webhook event with id, {} was not found", webhookEventId);
            return;
        }

        WebhookEvent event = webhookEvent.get();
        if(event.getStatus() == WebhookEventStatus.DELIVERED || event.getStatus() == WebhookEventStatus.DEAD){
            log.warn("cannot deliver the event {} in status: {}", webhookEventId, event.getStatus());
            return;
        }

        event.setAttempts(event.getAttempts()+1);
        event.setLastAttemptAt(LocalDateTime.now());


        try {

            var response = restClient.post()
                    .uri(event.getTargetUrl())
                    .header(signatureHeader, event.getSignature())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "event", event.getEventType(), "payload", event.getPayload()
                    )).retrieve()
                    .toBodilessEntity();

            int statusCode = response.getStatusCode().value();
            event.setLastResponseCode(statusCode);

            if(response.getStatusCode().is2xxSuccessful()){
                event.setStatus(WebhookEventStatus.DELIVERED);
                event.setDeliveredAt(LocalDateTime.now());
                webhookEventRepository.save(event);
                log.info("Successfully delivered the event {} in status: {}", webhookEventId, event.getStatus());
                return;
            }

            handleAttemptFailed(event, "HTTP"+statusCode);

        } catch (RestClientException e) {
            event.setLastResponseBody(e.getMessage());
            handleAttemptFailed(event, e.getMessage());
            log.error("Webhook event with id {} failed", webhookEventId, e);
        }

    }

    public void handleAttemptFailed(WebhookEvent event, String error) {
        event.setLastResponseBody(error);

        if(event.getAttempts() >= MAX_ATTEMPTS){
            event.setStatus(WebhookEventStatus.DEAD);
            webhookDlqRecorder.recordAfterAttemptsExhausted(event, error);
            return;
        }

        Duration backoff = BACKOFF.get(event.getAttempts()-1);
        LocalDateTime nextRetryAt = LocalDateTime.now().plus(backoff);
        event.setStatus(WebhookEventStatus.FAILED);
        event.setNextRetryAt(nextRetryAt);
        webhookEventRepository.save(event);

        webhookRetryQueue.enqueue(event.getId(), nextRetryAt);
        log.error("Handling attempt failed for webhook event {} with attempts: {} and next retry at: {}",
                event.getId(), event.getAttempts(), nextRetryAt);
    }



}
