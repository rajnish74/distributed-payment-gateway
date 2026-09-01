package com.rajnish.razorpay.settlement;

import com.rajnish.razorpay.client.MerchantServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
@Slf4j
@RequiredArgsConstructor
public class SettlementEngine {

    private final MerchantServiceClient  merchantServiceClient;
    private final SettlementTransactionExecutor  settlementTransactionExecutor;

    @Scheduled(cron = "0 0 23 * * *")
    @SchedulerLock(name = "opeartions-service-settlement-engine", lockAtMostFor = "2h", lockAtLeastFor = "1m")
    public void runScheduled(){
        log.info("Nightly settlement engine...");
        run();
    }

    public void run(){
        List<UUID> merchantIds = merchantServiceClient.listActiveMerchantIds();
        log.info("Processing into settlement for {} merchantIds", merchantIds.size());

        try(ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()){

            List<Future<?>> futures = new ArrayList<>();
            for(UUID merchantId : merchantIds){
                futures.add(executorService.submit(() -> {
                    settlementTransactionExecutor.processForMerchant(merchantId, LocalDate.now());
                }));
            }

            for(Future<?> f : futures){
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error while processing settlement for merchant {}", merchantIds, e);
                    throw new RuntimeException(e);
                }

            }
        }

        log.info("Finished into settlement for {} merchantIds", merchantIds.size());
    }
}
