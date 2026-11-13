package com.Ecommerce.user_service.dto;

public record UserResponse(
        Long id,
        String name,
        String email,
        String role
) {
}
