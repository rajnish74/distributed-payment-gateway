package com.rajnish.razorpay.exceptions;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RateLimitException extends RuntimeException {

    private final int retryAfterSeconds;
    private final int remaining;
    public RateLimitException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
        this.remaining = 0;
    }
}
