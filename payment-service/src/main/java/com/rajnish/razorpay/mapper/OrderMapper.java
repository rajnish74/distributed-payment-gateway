package com.rajnish.razorpay.mapper;


import com.rajnish.razorpay.dto.response.OrderResponse;
import com.rajnish.razorpay.entity.OrderRecord;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {

    OrderResponse toOrderResponse(OrderRecord orderRecord);
}
