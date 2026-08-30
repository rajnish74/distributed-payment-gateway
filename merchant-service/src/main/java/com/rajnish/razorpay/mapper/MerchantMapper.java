package com.rajnish.razorpay.mapper;


import com.rajnish.razorpay.dto.request.MerchantSignupRequest;
import com.rajnish.razorpay.dto.response.MerchantResponse;
import com.rajnish.razorpay.entity.Merchant;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MerchantMapper {

    Merchant toEntity(MerchantSignupRequest request);

    MerchantResponse toResponse(Merchant merchant);
}
