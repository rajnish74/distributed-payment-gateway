package com.rajnish.razorpay.webhook;

import com.rajnish.razorpay.entity.WebhookEvent;
import com.rajnish.razorpay.enums.WebhookEventStatus;
import com.rajnish.razorpay.repository.WebhookEventRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebhookDeliverySchedular {

    private final WebhookRetryQueue retryQueue;
    private final WebhookEventRepository webhookEventRepository;
    private final WebhookDeliverExecutor deliverExecutor;
    private ExecutorService virtualThreadExecutor;

    @PostConstruct
    public void init() {
        virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @PreDestroy
    public void shutdown() {
        virtualThreadExecutor.shutdown();
    }

    @Value("${app.webhook.delivery.poll-batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelay = 1000)
    public void pollAndDeliver(){
        Set<UUID> dueEvents = retryQueue.pollDue(batchSize);

        if(dueEvents.isEmpty()) return;

        for(UUID webhookEventId : dueEvents){
            virtualThreadExecutor.submit(() -> {
                deliverExecutor.deliver(webhookEventId);
            });


        }

    }

    @Scheduled(fixedDelay = 10000)
    public void reconcileFromDatabase(){
        LocalDateTime now = LocalDateTime.now();
        List<WebhookEvent> due = webhookEventRepository
                .findByStatusAndNextRetryAtBefore(WebhookEventStatus.PENDING, now);

        for(WebhookEvent event : due){
            retryQueue.enqueueIfAbsent(event.getId(), event.getNextRetryAt());

        }
    }


}
