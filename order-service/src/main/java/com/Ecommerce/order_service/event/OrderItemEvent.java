package com.Ecommerce.order_service.event;

public record OrderItemEvent(
        Long productId,
        Integer quantity,
        Double price
) {
}