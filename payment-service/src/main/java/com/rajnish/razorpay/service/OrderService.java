package com.rajnish.razorpay.service;


import com.rajnish.razorpay.dto.request.CreateOrderRequest;
import com.rajnish.razorpay.dto.response.OrderResponse;
import com.rajnish.razorpay.dto.response.PaymentResponse;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderResponse createOrder(UUID merchantId, CreateOrderRequest request);

    OrderResponse getById(UUID merchantId, UUID orderId);

    OrderResponse cancelOrder(UUID merchantId, UUID orderId);

    List<PaymentResponse> listPayments(UUID merchantId, UUID orderId);
}
