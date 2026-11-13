package com.Ecommerce.order_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(
        prefix = "resilience4j.ratelimiter.instances.order-service"
)
public record RateLimiterProperties(
        int limitForPeriod,
        Duration limitRefreshPeriod,
        Duration timeoutDuration
) {
}