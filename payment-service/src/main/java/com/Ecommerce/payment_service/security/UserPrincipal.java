package com.Ecommerce.payment_service.security;

public record UserPrincipal(
        Long userId,
        String email,
        String role
) {}
