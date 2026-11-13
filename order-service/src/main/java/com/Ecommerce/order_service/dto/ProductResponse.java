package com.Ecommerce.order_service.dto;

public record ProductResponse(
        Long id,
        String name,
        String description,
        Double price,
        Integer stock,
        String category
) {
}