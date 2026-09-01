package com.rajnish.razorpay.saga;

import com.rajnish.razorpay.dto.request.PaymentInitRequest;
import com.rajnish.razorpay.dto.response.PaymentResponse;
import com.rajnish.razorpay.entity.OrderRecord;
import com.rajnish.razorpay.entity.Payments;
import com.rajnish.razorpay.enums.EventAggregateType;
import com.rajnish.razorpay.enums.OrderStatus;
import com.rajnish.razorpay.enums.PaymentEvent;
import com.rajnish.razorpay.enums.PaymentStatus;
import com.rajnish.razorpay.exceptions.BusinessRuleViolationException;
import com.rajnish.razorpay.exceptions.ResourceNotFoundException;
import com.rajnish.razorpay.gateway.dto.PaymentResult;
import com.rajnish.razorpay.mapper.PaymentMapper;
import com.rajnish.razorpay.outbox.OutboxEventPublisher;
import com.rajnish.razorpay.repositories.OrderRepository;
import com.rajnish.razorpay.repositories.PaymentRepository;
import com.rajnish.razorpay.statemachine.PaymentTransitionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentAuthorizationRecorder {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentTransitionLogService  paymentTransitionLogService;
    private final OutboxEventPublisher eventPublisher;
    private final PaymentMapper  paymentMapper;

    @Transactional
    public Payments orderRecord(UUID merchantId, PaymentInitRequest request){
        OrderRecord order=orderRepository.findByIdAndMerchantIdForUpdate(request.orderId(),merchantId)
                .orElseThrow(()->new ResourceNotFoundException("Order", request.orderId()));

        if (order.getOrderStatus()!= OrderStatus.CREATED && order.getOrderStatus()!= OrderStatus.ATTEMPTED){
            throw new BusinessRuleViolationException("ORDER_NOT_PAYABLE",
                    "Order cannot accept payment in status: "+order.getOrderStatus());

        }

        order.setOrderStatus(OrderStatus.ATTEMPTED);
        order.setAttempts(order.getAttempts()+1);

        Payments payments=Payments.builder()
                .order(order)
                .merchantId(merchantId)
                .amount(order.getAmount())
                .status(PaymentStatus.CREATED)
                .method(request.method())
                .idempotencyKey(UUID.randomUUID().toString())
                .methodDetails(request.methodDetails())
                .build();

        payments = paymentRepository.save(payments);
        paymentTransitionLogService.apply(payments, PaymentEvent.AUTHORIZE_ATTEMPT);

        return payments;

    }

    @Transactional
    public PaymentResponse compensateAuthorizationFailure(UUID paymentId, String errorCode,
                                                          String errorDescription){
        Payments payments = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(()-> new ResourceNotFoundException("Payment", paymentId));

        paymentTransitionLogService.apply(payments, PaymentEvent.AUTHORIZE_FAILURE);
        payments.setErrorCode(errorCode);
        payments.setErrorMessage(errorDescription);
        payments  = paymentRepository.save(payments);

        publishStatusEvent(payments, "PAYMENT_AUTHORIZATION_COMPENSATED");
        return paymentMapper.toResponse(payments);
    }

    public PaymentResponse applyGatewayResult(UUID paymentId, PaymentResult result){
        log.info("Payment Gateway result for payment id {} is {}", paymentId, result);
        Payments payments = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(()-> new ResourceNotFoundException("Payment", paymentId));

        switch (result){
            case PaymentResult.Pending pending ->payments.setProcessorReference(pending.registrationRef());
            case PaymentResult.Failure failure ->{
                //payments.setStatus(PaymentStatus.FAILED);
                paymentTransitionLogService.apply(payments, PaymentEvent.AUTHORIZE_FAILURE);
                payments.setErrorCode(failure.errorCode());
                payments.setErrorMessage(failure.errorDescription());
            }
            case PaymentResult.Success success ->{
                log.warn("Invalid state: initiate() gateway call returned success directly, paymentId={}",paymentId);
            }

        }
        payments = paymentRepository.save(payments);
        publishStatusEvent(payments, "PAYMENT_CREATED");
        log.info("Successfully applied result for payment id {}", paymentId);
        return paymentMapper.toResponse(payments);


    }

    private void publishStatusEvent(Payments payments, String eventType){
        eventPublisher.publish(EventAggregateType.PAYMENT, payments.getId(), eventType,
                Map.of("orderId", payments.getOrder().getId().toString(),
                        "paymentId", payments.getId().toString(),
                        "merchantId", payments.getMerchantId().toString(),
                        "paymentStatus", payments.getStatus().name(),
                        "amountUnits", payments.getAmount().getAmountUnits(),
                        "amountCurrency", payments.getAmount().getCurrency(),
                        "paymentMethod", payments.getMethod()
                ));
    }
}
