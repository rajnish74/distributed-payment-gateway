package com.rajnish.razorpay.config;


import com.rajnish.razorpay.enums.PaymentMethod;
import com.rajnish.razorpay.gateway.PaymentAdapter;
import com.rajnish.razorpay.gateway.adapter.CardPaymentAdapter;
import com.rajnish.razorpay.gateway.adapter.NetBankingAdapter;
import com.rajnish.razorpay.gateway.adapter.UPIPaymentAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
@RequiredArgsConstructor
@Configuration
public class PaymentAdapterConfig {

    private final NetBankingAdapter netBankingAdapter;
    private final UPIPaymentAdapter upiPaymentAdapter;
    private final CardPaymentAdapter cardPaymentAdapter;

    @Bean
    public Map<PaymentMethod, PaymentAdapter> paymentAdapterMap(){
        return Map.of(
                PaymentMethod.CARD,cardPaymentAdapter,
                PaymentMethod.NETBANKING,netBankingAdapter,
                PaymentMethod.UPI,upiPaymentAdapter
        );
    }
}
