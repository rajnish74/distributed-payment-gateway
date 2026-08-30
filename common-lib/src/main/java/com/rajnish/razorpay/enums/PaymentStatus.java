package com.rajnish.razorpay.enums;

public enum PaymentStatus {
    CREATED,
    AUTHORIZING,
    AUTHORIZED,
    CAPTURING,
    CAPTURED,
    FAILED,
    REFUNDED,
    PARTIALLY_REFUNDED,
    CANCELLED,
    AUTH_EXPIRED,
    SETTLED,
}
