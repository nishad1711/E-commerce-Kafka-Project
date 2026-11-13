package com.Ecommerce.order_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/ecommerce_product_db",
        "spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}",
        "app.jwt.secret=${JWT_SECRET}",
        "app.internal-service.token=local-test-token",
        "eureka.client.enabled=false",
        "spring.kafka.listener.auto-startup=false"
})
class OrderServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}