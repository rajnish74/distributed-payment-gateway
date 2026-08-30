package com.rajnish.razorpay.simulator;


import com.rajnish.razorpay.entity.Payments;
import com.rajnish.razorpay.enums.ChaosMode;
import com.rajnish.razorpay.enums.PaymentStatus;
import com.rajnish.razorpay.repositories.PaymentRepository;
import com.rajnish.razorpay.service.PaymentService;
import com.rajnish.razorpay.utils.RandomizerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class BankCallbackSimulator {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final SimulatorConfig simulatorConfig;

//    @Scheduled(fixedDelayString = "${payment.simulator.poll-interval-ms:5000}")
    public void processCallbacks(){
        LocalDateTime globalWindow=LocalDateTime.now().minusSeconds(1);

        List<Payments> candidates=paymentRepository
                .findByStatusAndCreatedAtBefore(PaymentStatus.AUTHORIZING,globalWindow);

        log.info("Found {} candidates",candidates.size());

        if(candidates.isEmpty()){
            return;
        }

        for(Payments payments:candidates){
            simulateCallback(payments);
        }
    }

    private void simulateCallback(Payments payments) {
        SimulatorConfig.MethodSimulatorConfig methodConfig= simulatorConfig.configfor(payments.getMethod());

        LocalDateTime dueAt=dueAt(payments,methodConfig);

        if(LocalDateTime.now().isBefore(dueAt)){
            return;
        }

        ChaosMode chaosMode=simulatorConfig.getChaosMode();

        switch (chaosMode){
            case SUCCESS -> resolve(payments,true);
            case FAILURE -> resolve(payments,false);
            case TIMEOUT -> {
                log.debug("BankCallbackSimulator: Payments timeout");
            }
            case NORMAL,SLOW -> resolve(payments,shouldApproved(payments,methodConfig));

        }
    }

    private void resolve(Payments payments, boolean approve) {
        if(approve){
            String bankRef = "SIMULATE_BANK_REF"+ RandomizerUtil.randomBase64(8);
            paymentService.resolveAuthorization(payments.getId(), true, bankRef, null, null);
        }else {
            paymentService.resolveAuthorization(payments.getId(), false, "null", "SIMULATE_BANK_ERROR_CODE", "Simulate bank declined");
        }
    }

    private boolean shouldApproved(Payments payments, SimulatorConfig.MethodSimulatorConfig methodConfig) {
        int bucket=Math.abs(payments.getId().hashCode()) % 100;
        return bucket < methodConfig.getSuccessRate();
    }

    private LocalDateTime dueAt(Payments payments,SimulatorConfig.MethodSimulatorConfig methodConfig){

        int range=methodConfig.getMaxDelaySeconds()-methodConfig.getMinDelaySeconds();
        int delaySeconds=methodConfig.getMinDelaySeconds()+Math.abs(payments.getId().hashCode())%(range+1);

        if(simulatorConfig.getChaosMode() == ChaosMode.SLOW){
            delaySeconds *=2;
        }

        return payments.getCreatedAt().plusSeconds(delaySeconds);
    }

}
