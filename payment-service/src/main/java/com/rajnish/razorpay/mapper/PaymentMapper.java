package com.rajnish.razorpay.mapper;



import com.rajnish.razorpay.dto.response.PaymentResponse;
import com.rajnish.razorpay.entity.Payments;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentMapper {

   @Mapping(target = "orderId", source = "order.id")
   @Mapping(target = "errorDescriptions", source = "errorMessage")
   //@Mapping(target="merchant_id",source="merchantId")
   PaymentResponse toResponse(Payments payments);

   List<PaymentResponse> toResponseList(List<Payments> paymentsList);
}
