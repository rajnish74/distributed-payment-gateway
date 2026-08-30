package com.rajnish.razorpay.simulator;


import com.rajnish.razorpay.enums.ChaosMode;
import com.rajnish.razorpay.enums.PaymentMethod;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "payment.simulator")
@RequiredArgsConstructor
@Getter
@Setter
public class SimulatorConfig {

    private Integer pollIntervalMs=2000;
    private ChaosMode chaosMode=ChaosMode.NORMAL;

    private final Map<String, MethodSimulatorConfig> methods=new HashMap<>();


    public MethodSimulatorConfig configfor(PaymentMethod method) {
        return methods.getOrDefault(method.name(),new MethodSimulatorConfig());
    }

    @Getter
    @Setter
    public static class MethodSimulatorConfig {
        private Integer minDelaySeconds=1;
        private Integer maxDelaySeconds=5;
        private Integer successRate=80;
    }

}
