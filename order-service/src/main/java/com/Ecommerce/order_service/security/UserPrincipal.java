package com.Ecommerce.order_service.security;

public record UserPrincipal(
        Long userId,
        String email,
        String role
) {}
