package com.Ecommerce.order_service.event;

public record PaymentFailedEvent(
        Long paymentId,
        Long orderId,
        Long userId,
        Double amount
) {}