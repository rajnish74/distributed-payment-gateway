package com.rajnish.razorpay.statemachine;


import com.rajnish.razorpay.context.MerchantContext;
import com.rajnish.razorpay.entity.PaymentTransitionLog;
import com.rajnish.razorpay.entity.Payments;
import com.rajnish.razorpay.enums.PaymentActor;
import com.rajnish.razorpay.enums.PaymentEvent;
import com.rajnish.razorpay.enums.PaymentStatus;
import com.rajnish.razorpay.repositories.PaymentTransitionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentTransitionLogService {
    private final PaymentTransitionLogRepository paymentTransitionLogRepository;
    private final PaymentStateMachine paymentStateMachine;
    private final MerchantContext  merchantContext;

    public PaymentStatus apply(Payments payments, PaymentEvent event) {
        PaymentStatus next=paymentStateMachine.transition(payments.getStatus(),event);

        PaymentActor actor = getPaymentActor();
        PaymentTransitionLog log=PaymentTransitionLog.builder()
                .payments(payments)
                .fromStatus(payments.getStatus())
                .toStatus(next)
                .event(event)
                .actor(actor)
                .occurredAt(LocalDateTime.now())
                .build();

        payments.setStatus(next);
        paymentTransitionLogRepository.save(log);

        return next;
    }

    private PaymentActor getPaymentActor() {
        try{
            String keyId = merchantContext.getKeyId();
            UUID merchantId = merchantContext.getMerchantId();

            if(keyId != null && !keyId.isBlank()){
                return PaymentActor.CUSTOMER;
            } else if(merchantId != null){
                return PaymentActor.MERCHANT;
            }
        } catch(Exception ignored){

        }
        return PaymentActor.SYSTEM;
    }
}
