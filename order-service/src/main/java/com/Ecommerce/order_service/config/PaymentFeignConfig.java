package com.Ecommerce.order_service.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class PaymentFeignConfig {

    @Bean
    public RequestInterceptor paymentServiceTokenInterceptor(
            @Value("${app.internal-service.token}") String internalServiceToken) {

        return requestTemplate -> requestTemplate.header(
                "X-Internal-Service-Token",
                internalServiceToken
        );
    }
}
