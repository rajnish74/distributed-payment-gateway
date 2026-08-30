package com.rajnish.razorpay.services.impl;


import com.rajnish.razorpay.dto.request.LoginRequest;
import com.rajnish.razorpay.dto.request.MerchantSignupRequest;
import com.rajnish.razorpay.dto.response.LoginResponse;
import com.rajnish.razorpay.dto.response.MerchantResponse;
import com.rajnish.razorpay.entity.AppUser;
import com.rajnish.razorpay.entity.Merchant;
import com.rajnish.razorpay.enums.MerchantStatus;
import com.rajnish.razorpay.enums.UserRole;
import com.rajnish.razorpay.exceptions.BusinessRuleViolationException;
import com.rajnish.razorpay.exceptions.DuplicateResourceException;
import com.rajnish.razorpay.exceptions.ResourceNotFoundException;
import com.rajnish.razorpay.mapper.MerchantMapper;
import com.rajnish.razorpay.repository.AppUserRepository;
import com.rajnish.razorpay.repository.MerchantRepository;
import com.rajnish.razorpay.security.JwtUtils;
import com.rajnish.razorpay.services.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AppUserRepository appUserRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantMapper merchantMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    @Transactional
    public MerchantResponse signup(MerchantSignupRequest request) {
        if (merchantRepository.existsByEmail(request.email())){
            throw new DuplicateResourceException("DUPLICATE_MERCHANT_EMAIL","Merchant with email "+request.email()+" already exists");
        }

        Merchant merchant=merchantMapper.toEntity(request);
        merchant.setStatus(MerchantStatus.PENDING_KYC);

        merchant=merchantRepository.save(merchant);

        AppUser appUser= AppUser.builder()
                .email(request.email())
                .merchant(merchant)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.OWNER)
                .build();
        appUserRepository.save(appUser);


        return merchantMapper.toResponse(merchant);
    }

    @Override
    public LoginResponse login(LoginRequest request) {

        AppUser appUser=appUserRepository.findByEmail(request.email())
                .orElseThrow(()->new ResourceNotFoundException("USER_NOT_FOUND","User with email "+request.email()+" not found"));

        if (!passwordEncoder.matches(request.password(), appUser.getPasswordHash())){
            throw new BusinessRuleViolationException("INVALID_CREDENTIALS","Invalid email or password");
        }

        String token= jwtUtils.generateAccessToken(
                request.email(),
                appUser.getMerchant().getId(),
                appUser.getRole().toString()
        );
        return new LoginResponse(token);
    }
}
