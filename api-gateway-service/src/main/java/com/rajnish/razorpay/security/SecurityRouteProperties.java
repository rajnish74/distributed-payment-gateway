package com.rajnish.razorpay.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security")
public class SecurityRouteProperties {

    private List<String> publicRoutes = List.of();
}
