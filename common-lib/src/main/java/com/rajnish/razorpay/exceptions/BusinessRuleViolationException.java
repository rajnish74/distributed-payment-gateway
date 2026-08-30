package com.rajnish.razorpay.exceptions;

import lombok.Getter;

@Getter
public class BusinessRuleViolationException extends RuntimeException {
    private final String errorCode;
    public BusinessRuleViolationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
