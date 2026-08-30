package com.rajnish.razorpay.services;


import com.rajnish.razorpay.dto.request.LoginRequest;
import com.rajnish.razorpay.dto.request.MerchantSignupRequest;
import com.rajnish.razorpay.dto.response.LoginResponse;
import com.rajnish.razorpay.dto.response.MerchantResponse;

public interface AuthService {
    MerchantResponse signup(MerchantSignupRequest request);

    LoginResponse login(LoginRequest request);
}
