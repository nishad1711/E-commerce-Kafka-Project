package com.Ecommerce.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderRequest(

        Long userId,

        @NotEmpty
        List<@Valid OrderItemRequest> items
) {
}