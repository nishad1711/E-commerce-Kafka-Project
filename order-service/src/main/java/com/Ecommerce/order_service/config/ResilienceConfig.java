package com.Ecommerce.order_service.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties({RateLimiterProperties.class
, BulkheadProperties.class})
public class ResilienceConfig {

    @Bean
    public Retry paymentRetry(RetryRegistry retryRegistry) {
        return retryRegistry.retry("paymentService");
    }
    @Bean
    public RateLimiter orderRateLimiter(RateLimiterProperties properties) {

        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(properties.limitForPeriod())
                .limitRefreshPeriod(properties.limitRefreshPeriod())
                .timeoutDuration(properties.timeoutDuration())
                .build();

        return RateLimiter.of("orderService", config);
    }
    @Bean
    public Bulkhead paymentBulkhead(BulkheadProperties properties) {

        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(properties.maxConcurrentCalls())
                .maxWaitDuration(properties.maxWaitDuration())
                .build();

        return Bulkhead.of("paymentService", config);
    }
}