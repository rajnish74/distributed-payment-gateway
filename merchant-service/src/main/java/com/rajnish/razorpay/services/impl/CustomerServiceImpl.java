package com.rajnish.razorpay.services.impl;


import com.rajnish.razorpay.entity.Customer;
import com.rajnish.razorpay.entity.Merchant;
import com.rajnish.razorpay.exceptions.ResourceNotFoundException;
import com.rajnish.razorpay.repository.CustomerRepository;
import com.rajnish.razorpay.repository.MerchantRepository;
import com.rajnish.razorpay.services.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final MerchantRepository merchantRepository;


    @Override
    @Transactional
    public UUID findOrCreate(UUID merchantId, String email, String name, String phone) {

        if (email == null || email.isBlank()) {
            return null;
        }
        return customerRepository.findByMerchant_IdAndEmail(merchantId, email)
                .map(Customer::getId)
                .orElseGet(()-> createOne(merchantId, email, name, phone));
    }


    private UUID createOne(UUID merchantId, String email, String name, String phone) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId));

        Customer customer = Customer.builder()
                .merchant(merchant)
                .email(email)
                .name(name)
                .phone(phone)
                .build();

        customer =  customerRepository.save(customer);
        log.info("Created Customer via findOrCreate id={} merchantId={} email={}",
                customer.getId(),merchant.getId(), email);
        return customer.getId();
    }
}
