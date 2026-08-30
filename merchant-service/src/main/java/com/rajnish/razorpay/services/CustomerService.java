package com.rajnish.razorpay.services;

import java.util.UUID;

public interface CustomerService {

    UUID findOrCreate(UUID merchantId, String email, String name, String phone);
}
