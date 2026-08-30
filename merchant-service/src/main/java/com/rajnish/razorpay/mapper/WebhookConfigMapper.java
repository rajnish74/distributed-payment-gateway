package com.rajnish.razorpay.mapper;


import com.rajnish.razorpay.dto.response.WebhookConfigResponse;
import com.rajnish.razorpay.entity.MerchantWebhookConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WebhookConfigMapper {

    @Mapping(target = "webhookSecret", source = "rawSecret")
    WebhookConfigResponse toResponse(MerchantWebhookConfig merchantWebhookConfig, String rawSecret);
}
