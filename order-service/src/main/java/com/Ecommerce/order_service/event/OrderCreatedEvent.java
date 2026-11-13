package com.Ecommerce.order_service.event;

import java.util.List;

public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        Double totalAmount,
        List<OrderItemEvent> items
) {
}