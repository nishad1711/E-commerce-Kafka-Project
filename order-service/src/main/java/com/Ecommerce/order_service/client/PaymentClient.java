package com.Ecommerce.order_service.client;

import com.Ecommerce.order_service.dto.PaymentRequest;
import com.Ecommerce.order_service.config.PaymentFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "payment-service",
        configuration = PaymentFeignConfig.class
)
public interface PaymentClient {

    @PostMapping("/api/payments")
    Object createPayment(@RequestBody PaymentRequest request);
}