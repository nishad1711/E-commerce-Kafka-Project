package com.Ecommerce.order_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(
        prefix = "resilience4j.bulkhead.instances.payment-service"
)
public record BulkheadProperties(
        int maxConcurrentCalls,
        Duration maxWaitDuration
) {
}