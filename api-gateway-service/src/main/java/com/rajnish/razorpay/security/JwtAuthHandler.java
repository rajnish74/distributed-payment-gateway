package com.rajnish.razorpay.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthHandler {

    private final JwtVerifier jwtVerifier;

    public Map<String,String> authenticate(String token){
        Claims claims;
        try {
            claims = jwtVerifier.verify(token);
        } catch(Exception e) {
            throw new GatewayAuthenticationException("Invalid on expire token");
        }

        return Map.of(
                "X-Merchant-Id", jwtVerifier.extractMerchantId(claims),
                "X-User-Role", jwtVerifier.extractRole(claims)
        );
    }
}
