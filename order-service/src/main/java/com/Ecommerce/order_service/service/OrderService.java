
package com.Ecommerce.order_service.service;

import com.Ecommerce.order_service.client.PaymentClient;
import com.Ecommerce.order_service.client.ProductClient;
import com.Ecommerce.order_service.dto.CreateOrderRequest;
import com.Ecommerce.order_service.dto.OrderItemRequest;
import com.Ecommerce.order_service.dto.PaymentRequest;
import com.Ecommerce.order_service.dto.ProductResponse;
import com.Ecommerce.order_service.entity.Order;
import com.Ecommerce.order_service.entity.OrderItem;
import com.Ecommerce.order_service.entity.OrderStatus;
import com.Ecommerce.order_service.exception.PaymentServiceException;
import com.Ecommerce.order_service.exception.RateLimitException;
import com.Ecommerce.order_service.repository.OrderRepository;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final PaymentClient paymentClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;
    private final Retry paymentRetry;
    private final RateLimiter orderRateLimiter;
    private final Bulkhead paymentBulkhead;
    private final OrderPersistenceService orderPersistenceService;

    public OrderService(
            OrderRepository orderRepository,
            ProductClient productClient,
            PaymentClient paymentClient,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory,
            Retry paymentRetry,
            RateLimiter orderRateLimiter,
            Bulkhead paymentBulkhead,
            OrderPersistenceService orderPersistenceService
    ) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.paymentClient = paymentClient;
        this.circuitBreakerFactory = circuitBreakerFactory;
        this.paymentRetry = paymentRetry;
        this.orderRateLimiter = orderRateLimiter;
        this.paymentBulkhead = paymentBulkhead;
        this.orderPersistenceService = orderPersistenceService;
    }

    public Order createOrder(CreateOrderRequest request) {

        if (!orderRateLimiter.acquirePermission()) {
            throw new RateLimitException(
                    "Too many requests. Please try again later."
            );
        }

        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException(
                    "Order must contain at least one item"
            );
        }

        Order order = Order.builder()
                .userId(request.userId())
                .totalAmount(0.0)
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();

        double total = 0;
        List<OrderItemRequest> reservedItems = new ArrayList<>();

        try {
            for (OrderItemRequest itemRequest : request.items()) {

                if (itemRequest.quantity() <= 0) {
                    throw new IllegalArgumentException(
                            "Quantity must be greater than zero"
                    );
                }

                ProductResponse product =
                        productClient.getProductById(
                                itemRequest.productId()
                        );

                productClient.reserveStock(
                        itemRequest.productId(),
                        itemRequest.quantity()
                );

                reservedItems.add(itemRequest);

                OrderItem item = OrderItem.builder()
                        .productId(itemRequest.productId())
                        .quantity(itemRequest.quantity())
                        .price(product.price())
                        .order(order)
                        .build();

                order.getItems().add(item);

                total += product.price() * itemRequest.quantity();
            }
        } catch (Exception e) {
            releaseReservedStock(reservedItems);
            throw e;
        }

        order.setTotalAmount(total);

        Order savedOrder;

        try {
            savedOrder = orderPersistenceService.saveOrderWithOutbox(order);
        } catch (Exception e) {
            releaseReservedStock(reservedItems);
            throw e;
        }

        return savedOrder;
    }

    private void releaseReservedStock(
            List<OrderItemRequest> reservedItems
    ) {
        String rollbackId = UUID.randomUUID().toString();

        for (int i = 0; i < reservedItems.size(); i++) {
            OrderItemRequest item = reservedItems.get(i);

            try {
                productClient.releaseStock(
                        item.productId(),
                        item.quantity(),
                        "ROLLBACK-" + rollbackId + "-ITEM-" + i
                );
            } catch (Exception e) {
                System.err.println(
                        "Failed to release stock for product "
                                + item.productId()
                                + ". Manual recovery may be required."
                );
            }
        }
    }

    public void createPayment(PaymentRequest paymentRequest) {

        CircuitBreaker circuitBreaker =
                circuitBreakerFactory.create("paymentService");

        Supplier<Object> paymentCall = () ->
                circuitBreaker.run(
                        () -> {
                            paymentClient.createPayment(paymentRequest);
                            return null;
                        },
                        throwable -> {
                            throw new PaymentServiceException(
                                    "Payment service is unavailable. "
                                            + "Order cannot be processed."
                            );
                        }
                );

        Supplier<Object> retryablePaymentCall =
                Retry.decorateSupplier(paymentRetry, paymentCall);

        Supplier<Object> bulkheadPaymentCall =
                Bulkhead.decorateSupplier(
                        paymentBulkhead,
                        retryablePaymentCall
                );

        bulkheadPaymentCall.get();
    }

    public List<Order> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(
                        () -> new RuntimeException("Order not found")
                );
    }

    public Order updateStatus(Long id, OrderStatus status) {

        Order order = orderRepository.findById(id)
                .orElseThrow(
                        () -> new RuntimeException("Order not found")
                );

        order.setStatus(status);

        return orderRepository.save(order);
    }
}
