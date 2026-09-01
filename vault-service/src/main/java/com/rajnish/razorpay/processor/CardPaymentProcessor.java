package com.rajnish.razorpay.processor;


import com.rajnish.razorpay.dto.PaymentProcessorResponse;
import com.rajnish.razorpay.dto.PaymentProcessorRequest;
import com.rajnish.razorpay.utils.RandomizerUtil;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class CardPaymentProcessor  {

    public static final String PAN_CARD_DECLINED="4000000000000002";
    public static final String PAN_CARD_EXPIRED="4000000000000069";

    @Bulkhead(name = "vault-card-processor", type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<PaymentProcessorResponse> charge(PaymentProcessorRequest request) {

        String pan=request.pan();

        if (PAN_CARD_DECLINED.equals(pan)) {
            log.warn("CARD DECLINED");
            return CompletableFuture.completedFuture(new PaymentProcessorResponse.Failure("CARD_DECLINED", "The card was declined."));

        }
        if (PAN_CARD_EXPIRED.equals(pan)) {
            log.warn("CARD EXPIRED");
            return CompletableFuture.completedFuture(new PaymentProcessorResponse.Failure("CARD_EXPIRED", "The card has expired."));
        }


        String processorRef="CARD_PROCESSOR_"+ RandomizerUtil.randomBase64(16);

        return CompletableFuture.completedFuture(new PaymentProcessorResponse.Pending(processorRef));
    }
}
