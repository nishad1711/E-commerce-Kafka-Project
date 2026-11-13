
package com.Ecommerce.order_service.client;

import com.Ecommerce.order_service.dto.ProductResponse;
import com.Ecommerce.order_service.config.ProductFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "product-service",
        configuration = ProductFeignConfig.class
)
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductResponse getProductById(
            @PathVariable Long id
    );

    @PostMapping("/api/products/{id}/reserve-stock")
    void reserveStock(
            @PathVariable Long id,
            @RequestParam("quantity") int quantity
    );
    @PostMapping("/api/products/{id}/release-stock")
    void releaseStock(
            @PathVariable Long id,
            @RequestParam("quantity") int quantity,
            @RequestParam("operationKey") String operationKey
    );
}
