package com.Ecommerce.product_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductRequest(

        @NotBlank
        String name,

        @NotBlank
        String description,

        @NotNull
        @Min(0)
        Double price,

        @NotNull
        @Min(0)
        Integer stock,

        @NotBlank
        String category
) {
}