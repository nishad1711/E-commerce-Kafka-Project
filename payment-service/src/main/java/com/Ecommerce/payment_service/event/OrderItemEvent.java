package com.Ecommerce.payment_service.event;

public record OrderItemEvent(
        Long productId,
        Integer quantity,
        Double price
) {
}