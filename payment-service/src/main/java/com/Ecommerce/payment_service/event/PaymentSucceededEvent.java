package com.Ecommerce.payment_service.event;

public record PaymentSucceededEvent(
        Long paymentId,
        Long orderId,
        Long userId,
        Double amount
) {}