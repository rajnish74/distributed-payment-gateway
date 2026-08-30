package com.rajnish.razorpay.security;

public class GatewayAuthenticationException extends RuntimeException {
    public GatewayAuthenticationException(String message) {
        super(message);
    }
}
