package com.rajnish.razorpay.processor;


import com.rajnish.razorpay.dto.PaymentProcessorResponse;
import com.rajnish.razorpay.dto.PaymentProcessorRequest;
import com.rajnish.razorpay.utils.RandomizerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CardPaymentProcessor  {

    public static final String PAN_CARD_DECLINED="4000000000000002";
    public static final String PAN_CARD_EXPIRED="4000000000000069";

    public PaymentProcessorResponse charge(PaymentProcessorRequest request) {

        String pan=request.pan();

        if (PAN_CARD_DECLINED.equals(pan)) {
            log.warn("CARD DECLINED");
            return new PaymentProcessorResponse.Failure("CARD_DECLINED", "The card was declined.");

        }
        if (PAN_CARD_EXPIRED.equals(pan)) {
            log.warn("CARD EXPIRED");
            return new PaymentProcessorResponse.Failure("CARD_EXPIRED", "The card has expired.");
        }


        String processorRef="CARD_PROCESSOR_"+ RandomizerUtil.randomBase64(16);

        return new PaymentProcessorResponse.Pending(processorRef);
    }
}
