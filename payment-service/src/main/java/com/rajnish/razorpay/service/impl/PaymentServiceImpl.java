package com.rajnish.razorpay.service.impl;


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
import com.rajnish.razorpay.gateway.PaymentGatewayRouter;
import com.rajnish.razorpay.gateway.dto.PaymentRequest;
import com.rajnish.razorpay.gateway.dto.PaymentResult;
import com.rajnish.razorpay.mapper.PaymentMapper;
import com.rajnish.razorpay.outbox.OutboxEventPublisher;
import com.rajnish.razorpay.repositories.OrderRepository;
import com.rajnish.razorpay.repositories.PaymentRepository;
import com.rajnish.razorpay.saga.PaymentAuthorizationRecorder;
import com.rajnish.razorpay.service.PaymentService;
import com.rajnish.razorpay.statemachine.PaymentTransitionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGatewayRouter paymentGatewayRouter;
    private final PaymentMapper paymentMapper;
    private final PaymentTransitionLogService paymentTransitionLogService;
    private final OutboxEventPublisher eventPublisher;
    private final PaymentAuthorizationRecorder  paymentAuthorizationRecorder;

    @Override
    public PaymentResponse initiate(UUID merchantId, PaymentInitRequest request, String idempotencyKey) {

        if(idempotencyKey != null){
            var existing = paymentAuthorizationRecorder.findExistingAttempt(merchantId, idempotencyKey);
            if(existing.isPresent()){
                log.info("Idempotency replay for paymentId: {}", existing.get().id());
                return existing.get();
            }
        }

        Payments payments = paymentAuthorizationRecorder.orderRecord(merchantId, request, idempotencyKey);

        PaymentRequest paymentRequest=new PaymentRequest(
                payments.getId(),
                request.orderId(),
                merchantId,
                payments.getAmount(),
                request.method(),
                request.methodDetails()
        );

        PaymentResult result;
        try{
            result = paymentGatewayRouter.initiate(paymentRequest);
        } catch (Exception e){
            return paymentAuthorizationRecorder.compensateAuthorizationFailure(payments.getId(),
                    "PAYMENT_GATEWAY_ROUTER_UNREACHABLE", e.getMessage());
        }

        return paymentAuthorizationRecorder.applyGatewayResult(payments.getId(), result);

    }

    @Override
    @Transactional
    public PaymentResponse capture(UUID merchantId, UUID paymentId) {

//        Payments payments=paymentRepository.findByIdAndMerchantId(paymentId,merchantId)
//                .orElseThrow(()->new ResourceNotFoundException("Payment", paymentId));

        Payments payments=paymentRepository.findByIdAndMerchantIdForUpdate(paymentId,merchantId)
                .orElseThrow(()->new ResourceNotFoundException("Payment", paymentId));

       paymentTransitionLogService.apply(payments, PaymentEvent.CAPTURE_REQUEST);

        PaymentResult paymentResult=paymentGatewayRouter.capture(payments.getMethod(),paymentId);

        if (paymentResult instanceof PaymentResult.Success success) {
           paymentTransitionLogService.apply(payments, PaymentEvent.CAPTURE_SUCCESS);
            payments.setCapturedAt(LocalDateTime.now());
            log.info("Payment captured successfully, paymentId: {}",paymentId);
        }else if (paymentResult instanceof PaymentResult.Failure failure) {
            paymentTransitionLogService.apply(payments, PaymentEvent.CAPTURE_FAILURE);
            payments.setErrorCode(failure.errorCode());
            payments.setErrorMessage(failure.errorDescription());
            log.warn("Payment failed, paymentId: {}",paymentId);

        }

        payments=paymentRepository.save(payments);

        eventPublisher.publish(EventAggregateType.PAYMENT, payments.getId(), "PAYMENT_STATUS_CHANGED",
                Map.of("orderId", payments.getOrder().getId().toString(),
                        "paymentId", payments.getId().toString(),
                        "merchantId", merchantId.toString(),
                        "paymentStatus", payments.getStatus().name(),
                        "amountUnits", payments.getAmount().getAmountUnits(),
                        "amountCurrency", payments.getAmount().getCurrency(),
                        "paymentmethod", payments.getMethod()
                )
        );

        return paymentMapper.toResponse(payments);
    }

    @Override
    @Transactional
    public void resolveAuthorization(UUID paymentId, boolean approve, String bankRef,
                                     String errorCode, String errorDescription) {

//        Payments payments=paymentRepository.findById(paymentId)
//                .orElseThrow(()->new ResourceNotFoundException("Payment", paymentId));

        Payments payments=paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(()->new ResourceNotFoundException("Payment", paymentId));

        if (payments.getStatus() != PaymentStatus.AUTHORIZING){
            log.warn("Payment is not in Authorizing state, paymentID: {}, status: {}",payments.getId(),payments.getStatus());
            return;
        }

        OrderRecord orderRecord=payments.getOrder();

        if (approve){
            paymentTransitionLogService.apply(payments,PaymentEvent.AUTHORIZE_SUCCESS);
            payments.setBankReference(bankRef);
            payments.setAuthorizedAt(LocalDateTime.now());

//            Auto-capture
            paymentTransitionLogService.apply(payments, PaymentEvent.CAPTURE_REQUEST);
            PaymentResult captureResult=paymentGatewayRouter.capture(payments.getMethod(),paymentId);

            if (captureResult instanceof PaymentResult.Success success) {
                paymentTransitionLogService.apply(payments, PaymentEvent.CAPTURE_SUCCESS);
                payments.setCapturedAt(LocalDateTime.now());
                orderRecord.setOrderStatus(OrderStatus.PAID);
            } else if (captureResult instanceof PaymentResult.Failure failure) {
                paymentTransitionLogService.apply(payments, PaymentEvent.CAPTURE_FAILURE);
                payments.setErrorCode(failure.errorCode());
                payments.setErrorMessage(failure.errorDescription());
            }

        } else {
            paymentTransitionLogService.apply(payments, PaymentEvent.AUTHORIZE_FAILURE);
            payments.setErrorCode(errorCode);
            payments.setErrorMessage(errorDescription);
        }

        paymentRepository.save(payments);

        eventPublisher.publish(EventAggregateType.PAYMENT, payments.getId(), "PAYMENT_STATUS_CHANGED",
                Map.of("orderId", payments.getOrder().getId().toString(),
                        "paymentId", payments.getId().toString(),
                        "merchantId", payments.getMerchantId().toString(),
                        "paymentStatus", payments.getStatus().name(),
                        "amountUnits", payments.getAmount().getAmountUnits(),
                        "amountCurrency", payments.getAmount().getCurrency(),
                        "paymentmethod", payments.getMethod()
                )
        );

        orderRepository.save(orderRecord);

    }
}
