package com.Ecommerce.payment_service.dto;

import com.Ecommerce.payment_service.entity.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(

        @NotNull
        Long orderId,

        @NotNull
        Long userId,

        @NotNull
        @Min(1)
        Double amount,

        @NotNull
        PaymentMethod paymentMethod
) {}