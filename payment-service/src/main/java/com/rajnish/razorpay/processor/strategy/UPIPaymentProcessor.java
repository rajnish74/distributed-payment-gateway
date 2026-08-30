package com.rajnish.razorpay.processor.strategy;


import com.rajnish.razorpay.dto.PaymentProcessorResponse;
import com.rajnish.razorpay.processor.PaymentProcessor;
import com.rajnish.razorpay.dto.PaymentProcessorRequest;
import com.rajnish.razorpay.utils.RandomizerUtil;
import org.springframework.stereotype.Component;

@Component
public class UPIPaymentProcessor implements PaymentProcessor {
    @Override
    public PaymentProcessorResponse charge(PaymentProcessorRequest request) {
        final String VPA_CODE_FAIL="fail@axis";
        String bankCode=request.methodDetails() != null ?
                request.methodDetails().get("vpa").toString() : null;

        //simulation
        if(VPA_CODE_FAIL.equals(bankCode)){
            return new PaymentProcessorResponse.Failure(
                    "UPI_FAILED",
                    "Bank failed to process the payment"
            );
        }

        String processorRef="UPI_PROCESSOR_"+ RandomizerUtil.randomBase64(16);

        return new PaymentProcessorResponse.Pending(processorRef);
    }
}
