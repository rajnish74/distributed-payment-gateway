package com.rajnish.razorpay.validation;


import com.rajnish.razorpay.dto.request.TokenizeRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.YearMonth;

public class ExpiryYearValidator
        implements ConstraintValidator<ExpiryYear, TokenizeRequest> {


    @Override
    public boolean isValid(
            TokenizeRequest request,
            ConstraintValidatorContext context
    ) {

        if (request == null) {
            return false;
        }


        Integer expiryMonth = request.expiryMonth();
        Integer expiryYear = request.expiryYear();


        if (expiryMonth == null || expiryYear == null) {
            return false;
        }


        YearMonth currentDate = YearMonth.now();

        YearMonth cardExpiryDate =
                YearMonth.of(expiryYear, expiryMonth);


        return !cardExpiryDate.isBefore(currentDate);
    }
}