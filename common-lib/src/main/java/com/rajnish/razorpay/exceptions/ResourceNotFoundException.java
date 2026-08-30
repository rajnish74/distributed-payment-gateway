package com.rajnish.razorpay.exceptions;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException{

    public final String resourceName;
    private final Object identifier;

    public ResourceNotFoundException(String resourceName, Object identifier){
        super("Resource "+resourceName+" with identifier "+identifier+" not found");
        this.resourceName = resourceName;
        this.identifier = identifier;
    }
}
