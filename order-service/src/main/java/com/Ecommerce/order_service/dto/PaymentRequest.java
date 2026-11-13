package com.Ecommerce.order_service.dto;

public record PaymentRequest(
        Long orderId,
        Long userId,
        Double amount,
        String paymentMethod
) {}