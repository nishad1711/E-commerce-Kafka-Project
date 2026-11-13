package com.Ecommerce.payment_service.event;

public record PaymentFailedEvent(
        Long paymentId,
        Long orderId,
        Long userId,
        Double amount
) {}
