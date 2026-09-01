package com.rajnish.razorpay.service.impl;


import com.rajnish.razorpay.client.CustomerServiceClient;
import com.rajnish.razorpay.dto.FindOrCreateCustomerRequest;
import com.rajnish.razorpay.dto.request.CreateOrderRequest;
import com.rajnish.razorpay.dto.response.OrderResponse;
import com.rajnish.razorpay.dto.response.PaymentResponse;
import com.rajnish.razorpay.entity.OrderRecord;
import com.rajnish.razorpay.entity.Payments;
import com.rajnish.razorpay.enums.EventAggregateType;
import com.rajnish.razorpay.enums.OrderStatus;
import com.rajnish.razorpay.exceptions.BusinessRuleViolationException;
import com.rajnish.razorpay.exceptions.DuplicateResourceException;
import com.rajnish.razorpay.exceptions.ResourceNotFoundException;
import com.rajnish.razorpay.mapper.OrderMapper;
import com.rajnish.razorpay.mapper.PaymentMapper;
import com.rajnish.razorpay.outbox.OutboxEventPublisher;
import com.rajnish.razorpay.repositories.OrderRepository;
import com.rajnish.razorpay.repositories.PaymentRepository;
import com.rajnish.razorpay.service.OrderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final CustomerServiceClient  customerServiceClient;
    private final OutboxEventPublisher eventPublisher;

    @Value("${order.default-expiry-minutes:30}")
    private int defaultOrderExpiryMinutes;

    @Override
    @Transactional
    @CircuitBreaker(name = "merchant-service")
    @Retry(name = "merchant-service ")
    public OrderResponse createOrder(UUID merchantId, CreateOrderRequest request) {
        if (request.receipt() != null && orderRepository.existsByMerchantIdAndReceipt(merchantId,request.receipt())){
            throw new DuplicateResourceException("DUPLICATE_ORDER_RECEIPT","Order with receipt already exists : "+request.receipt());
        }

        UUID customerId = null;
        if (request.customer() != null) {
            customerId = customerServiceClient.findOrCreate(
                    new FindOrCreateCustomerRequest(merchantId,
                            request.customer().email(),
                            request.customer().name(),
                            request.customer().phone())

                    );
        }

        OrderRecord order=OrderRecord.builder()
                .receipt(request.receipt())
                .amount(request.amount())
                .notes(request.notes())

                .merchantId(merchantId)
                .customerId(customerId)
                .orderStatus(OrderStatus.CREATED)
                .expiresAt(request.expiresAt() !=null ? request.expiresAt() :
                        LocalDateTime.now().plusMinutes(defaultOrderExpiryMinutes))
                .build();

        order=orderRepository.save(order);

        eventPublisher.publish(EventAggregateType.ORDER, order.getId(), "ORDER_CREATED",
                Map.of("orderId", order.getId(),
                        "merchantId", merchantId.toString(),
                        "orderStatus", order.getOrderStatus().name(),
                        "amountUnits", order.getAmount().getAmountUnits(),
                        "amountCurrency", order.getAmount().getCurrency()
                    )
                );

        return orderMapper.toOrderResponse(order);
    }

    @Override
    public OrderResponse getById(UUID merchantId, UUID orderId) {
        OrderRecord order= orderRepository.findByIdAndMerchantId(merchantId,orderId)
                .orElseThrow(()->new ResourceNotFoundException("order", orderId));
        return orderMapper.toOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID merchantId, UUID orderId) {
        OrderRecord order= orderRepository.findByIdAndMerchantId(merchantId,orderId)
                .orElseThrow(()->new ResourceNotFoundException("order", orderId));

        if (order.getOrderStatus()==OrderStatus.CANCELED || order.getOrderStatus()==OrderStatus.PAID) {
            throw new BusinessRuleViolationException("ORDER_CANNOT_BE_CANCELED",
                    "Order cannot be canceled in current state : "+order.getOrderStatus().name());
        }
        order.setOrderStatus(OrderStatus.CANCELED);
        order=orderRepository.save(order);

        eventPublisher.publish(EventAggregateType.ORDER, order.getId(), "ORDER_CANCELLED",
                Map.of("orderId", order.getId(),
                        "merchantId", merchantId.toString(),
                        "orderStatus", order.getOrderStatus().name(),
                        "amountUnits", order.getAmount().getAmountUnits(),
                        "amountCurrency", order.getAmount().getCurrency()
                )
        );

        return orderMapper.toOrderResponse(order);
    }

    @Override
    public List<PaymentResponse> listPayments(UUID merchantId, UUID orderId) {
        OrderRecord order= orderRepository.findByIdAndMerchantId(merchantId,orderId)
                .orElseThrow(()->new ResourceNotFoundException("order", orderId));

        List<Payments> paymentsList= paymentRepository.findByOrder_Id(order);

//        return paymentsList.stream().map(
//                payments->paymentMapper.toResponse(payments)
//        ).collect(Collectors.toList());

        return paymentMapper.toResponseList(paymentsList);
    }
}
