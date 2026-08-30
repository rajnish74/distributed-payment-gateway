package com.rajnish.razorpay.controller;

import com.rajnish.razorpay.dto.request.LoginRequest;
import com.rajnish.razorpay.dto.request.MerchantSignupRequest;
import com.rajnish.razorpay.dto.response.LoginResponse;
import com.rajnish.razorpay.dto.response.MerchantResponse;

import com.rajnish.razorpay.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<MerchantResponse> signup(@RequestBody @Valid MerchantSignupRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(
                authService.signup(request)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request){
        return ResponseEntity.status(HttpStatus.OK).body(
                authService.login(request)
        );
    }
}
