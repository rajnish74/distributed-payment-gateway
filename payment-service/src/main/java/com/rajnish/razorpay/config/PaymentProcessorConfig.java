package com.rajnish.razorpay.config;


import com.rajnish.razorpay.enums.PaymentMethod;
import com.rajnish.razorpay.processor.PaymentProcessor;
import com.rajnish.razorpay.processor.strategy.CardPaymentProcessor;
import com.rajnish.razorpay.processor.strategy.NetBankingPaymentProcessor;
import com.rajnish.razorpay.processor.strategy.UPIPaymentProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class PaymentProcessorConfig {

    private final CardPaymentProcessor cardPaymentProcessor;
    private final UPIPaymentProcessor upiPaymentProcessor;
    private final NetBankingPaymentProcessor netBankingPaymentProcessor;

    @Bean
    public Map<PaymentMethod, PaymentProcessor> paymentProcessorMap() {
        return Map.of(
                PaymentMethod.CARD,cardPaymentProcessor,
                PaymentMethod.NETBANKING,netBankingPaymentProcessor,
                PaymentMethod.UPI,upiPaymentProcessor
        );
    }

}
