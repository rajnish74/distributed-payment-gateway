package com.rajnish.razorpay.mapper;

import com.rajnish.razorpay.dto.response.ApiKeyCreateResponse;
import com.rajnish.razorpay.dto.response.ApiKeyResponse;
import com.rajnish.razorpay.entity.ApiKey;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ApiKeyMapper {

    ApiKeyCreateResponse toCreateResponse(ApiKey apiKey);

    List<ApiKeyResponse> toResponseList(List<ApiKey> apiKeysList);
}
