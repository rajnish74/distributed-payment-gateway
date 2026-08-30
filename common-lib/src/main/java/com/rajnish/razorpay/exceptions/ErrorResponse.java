package com.rajnish.razorpay.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String errorCode,
        String errorDescriptions,
        LocalDateTime timeStamp,
        List<FieldError> fieldError
) {
    public record FieldError(String field, String message) { }

    public static ErrorResponse of(String errorCode,String errorDescriptions){
        return new ErrorResponse(errorCode, errorDescriptions, LocalDateTime.now(), null);
    }

    public static ErrorResponse of(String errorCode,String errorDescriptions,List<FieldError> fieldErrors){
        return new ErrorResponse(errorCode, errorDescriptions, LocalDateTime.now(), fieldErrors);
    }
}
