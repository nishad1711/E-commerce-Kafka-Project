package com.Ecommerce.payment_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/ecommerce_payment_db",
        "spring.datasource.username=postgres",
        "spring.datasource.password=0000",
        "spring.kafka.listener.auto-startup=false",
        "eureka.client.enabled=false"
})
class PaymentServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}