package com.rajnish.razorpay.processor.strategy;


import com.rajnish.razorpay.dto.PaymentProcessorResponse;
import com.rajnish.razorpay.processor.PaymentProcessor;
import com.rajnish.razorpay.dto.PaymentProcessorRequest;
import com.rajnish.razorpay.utils.RandomizerUtil;
import org.springframework.stereotype.Component;

@Component
public class NetBankingPaymentProcessor implements PaymentProcessor {

    @Override
    public PaymentProcessorResponse charge(PaymentProcessorRequest request) {

        final String BANK_CODE_FAIL="BANK_CODE_FAIL";
        String bankCode=request.methodDetails() != null ?
                request.methodDetails().get("bank").toString() : null;

        //simulation
        if(BANK_CODE_FAIL.equals(bankCode)){
            return new PaymentProcessorResponse.Failure(
                    "BANK_FAILED",
                    "Bank failed to process the payment"
            );
        }

        String processorRef="NBK_PROCESSOR_"+ RandomizerUtil.randomBase64(16);
//        String redirectRef="http://REDIRECT_BANK.com/"+processorRef;

        return new PaymentProcessorResponse.Pending(processorRef);
    }
}
