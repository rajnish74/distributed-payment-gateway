package com.rajnish.razorpay.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;


@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ExpiryYearValidator.class)
public @interface ExpiryYear {


    String message() default
            "Invalid card expiry date";


    Class<?>[] groups() default {};


    Class<? extends Payload>[] payload() default {};

}