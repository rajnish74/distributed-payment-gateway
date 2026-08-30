package com.rajnish.razorpay.audit;

import com.rajnish.razorpay.context.MerchantContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;

@AutoConfiguration
public class SharedAuditAutoConfiguration {

    @Bean("auditAwareImpl")
    public AuditorAware<String> auditorAwareImpl(MerchantContext  merchantContext) {
        return new AuditorAwareImpl(merchantContext);
    }
}
