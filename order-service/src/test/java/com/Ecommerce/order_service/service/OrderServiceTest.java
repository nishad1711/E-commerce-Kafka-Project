
package com.Ecommerce.order_service.service;

import com.Ecommerce.order_service.client.PaymentClient;
import com.Ecommerce.order_service.client.ProductClient;
import com.Ecommerce.order_service.dto.CreateOrderRequest;
import com.Ecommerce.order_service.dto.OrderItemRequest;
import com.Ecommerce.order_service.dto.ProductResponse;
import java.util.function.Function;
import com.Ecommerce.order_service.entity.Order;
import com.Ecommerce.order_service.entity.OrderStatus;
import com.Ecommerce.order_service.exception.PaymentServiceException;
import com.Ecommerce.order_service.exception.RateLimitException;
import com.Ecommerce.order_service.repository.OrderRepository;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import com.Ecommerce.order_service.dto.PaymentRequest;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import java.util.function.Supplier;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Mock
    private Retry paymentRetry;

    @Mock
    private RateLimiter orderRateLimiter;

    @Mock
    private Bulkhead paymentBulkhead;

    @Mock
    private OrderPersistenceService orderPersistenceService;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        lenient().when(orderRateLimiter.acquirePermission()).thenReturn(true);
    }

    @Test
    void createOrderSuccessfully() {
        CreateOrderRequest request = new CreateOrderRequest(
                1L,
                List.of(new OrderItemRequest(2L, 3))
        );

        ProductResponse product = new ProductResponse(
                2L, "Keyboard", "Mechanical keyboard",
                2499.0, 20, "Electronics"
        );

        when(productClient.getProductById(2L)).thenReturn(product);

        when(orderPersistenceService.saveOrderWithOutbox(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.createOrder(request);

        assertEquals(1L, result.getUserId());
        assertEquals(7497.0, result.getTotalAmount());
        assertEquals(OrderStatus.CREATED, result.getStatus());
        assertEquals(1, result.getItems().size());
        assertEquals(2L, result.getItems().get(0).getProductId());
        assertEquals(3, result.getItems().get(0).getQuantity());

        verify(productClient).reserveStock(2L, 3);
        verify(orderPersistenceService).saveOrderWithOutbox(any(Order.class));
        verify(productClient, never()).releaseStock(anyLong(), anyInt(), anyString());
    }

    @Test
    void createOrderRejectsEmptyItems() {
        CreateOrderRequest request = new CreateOrderRequest(1L, List.of());

        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.createOrder(request)
        );

        verifyNoInteractions(productClient, orderPersistenceService);
    }

    @Test
    void createOrderRejectsZeroQuantity() {
        CreateOrderRequest request = new CreateOrderRequest(
                1L,
                List.of(new OrderItemRequest(2L, 0))
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.createOrder(request)
        );

        verifyNoInteractions(productClient, orderPersistenceService);
    }

    @Test
    void createOrderReleasesPreviouslyReservedStockWhenNextReservationFails() {
        CreateOrderRequest request = new CreateOrderRequest(
                1L,
                List.of(
                        new OrderItemRequest(2L, 2),
                        new OrderItemRequest(3L, 1)
                )
        );

        ProductResponse firstProduct = new ProductResponse(
                2L, "Keyboard", "Mechanical keyboard",
                2499.0, 20, "Electronics"
        );

        ProductResponse secondProduct = new ProductResponse(
                3L, "Mouse", "Wireless mouse",
                999.0, 20, "Electronics"
        );

        when(productClient.getProductById(2L)).thenReturn(firstProduct);
        when(productClient.getProductById(3L)).thenReturn(secondProduct);

        doAnswer(invocation -> {
            Long productId = invocation.getArgument(0);

            if (productId.equals(3L)) {
                throw new RuntimeException("Insufficient stock");
            }

            return null;
        }).when(productClient).reserveStock(anyLong(), anyInt());

        assertThrows(
                RuntimeException.class,
                () -> orderService.createOrder(request)
        );

        verify(productClient).reserveStock(2L, 2);
        verify(productClient).reserveStock(3L, 1);
        verify(productClient).releaseStock(
                eq(2L), eq(2), startsWith("ROLLBACK-")
        );

        verify(orderPersistenceService, never())
                .saveOrderWithOutbox(any(Order.class));
    }

    @Test
    void createOrderReleasesStockWhenPersistenceFails() {
        CreateOrderRequest request = new CreateOrderRequest(
                1L,
                List.of(new OrderItemRequest(2L, 2))
        );

        ProductResponse product = new ProductResponse(
                2L, "Keyboard", "Mechanical keyboard",
                2499.0, 20, "Electronics"
        );

        when(productClient.getProductById(2L)).thenReturn(product);

        when(orderPersistenceService.saveOrderWithOutbox(any(Order.class)))
                .thenThrow(new RuntimeException("Database failure"));

        assertThrows(
                RuntimeException.class,
                () -> orderService.createOrder(request)
        );

        verify(productClient).reserveStock(2L, 2);
        verify(productClient).releaseStock(
                eq(2L), eq(2), startsWith("ROLLBACK-")
        );
    }

    @Test
    void createOrderRejectsRequestWhenRateLimitIsReached() {
        when(orderRateLimiter.acquirePermission()).thenReturn(false);

        CreateOrderRequest request = new CreateOrderRequest(
                1L,
                List.of(new OrderItemRequest(2L, 1))
        );

        assertThrows(
                RateLimitException.class,
                () -> orderService.createOrder(request)
        );

        verifyNoInteractions(productClient, orderPersistenceService);
    }
    @Test
    void getOrdersByUserReturnsUserOrders() {
        Order order = Order.builder()
                .id(1L)
                .userId(10L)
                .status(OrderStatus.CREATED)
                .build();

        when(orderRepository.findByUserId(10L))
                .thenReturn(List.of(order));

        List<Order> result = orderService.getOrdersByUser(10L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getUserId());

        verify(orderRepository).findByUserId(10L);
    }

    @Test
    void getOrderByIdReturnsOrderWhenFound() {
        Order order = Order.builder()
                .id(1L)
                .userId(10L)
                .status(OrderStatus.CREATED)
                .build();

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        Order result = orderService.getOrderById(1L);

        assertEquals(1L, result.getId());
        assertEquals(10L, result.getUserId());

        verify(orderRepository).findById(1L);
    }

    @Test
    void getOrderByIdThrowsExceptionWhenNotFound() {
        when(orderRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderService.getOrderById(99L)
        );

        assertEquals("Order not found", exception.getMessage());
    }
    @Test
    void updateStatusUpdatesAndSavesOrder() {
        Order order = Order.builder()
                .id(1L)
                .userId(10L)
                .status(OrderStatus.CREATED)
                .build();

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.updateStatus(1L, OrderStatus.PAID);

        assertEquals(OrderStatus.PAID, result.getStatus());

        verify(orderRepository).findById(1L);
        verify(orderRepository).save(order);
    }

    @Test
    void updateStatusThrowsExceptionWhenOrderNotFound() {
        when(orderRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderService.updateStatus(99L, OrderStatus.PAID)
        );

        assertEquals("Order not found", exception.getMessage());

        verify(orderRepository, never()).save(any(Order.class));
    }
    @Test
    void createPaymentSuccessfully() {
        PaymentRequest request = new PaymentRequest(
                1L, 10L, 2499.0, "CARD"
        );

        CircuitBreaker circuitBreaker = mock(CircuitBreaker.class);

        when(circuitBreakerFactory.create("paymentService"))
                .thenReturn(circuitBreaker);

        when(circuitBreaker.run(
                any(Supplier.class),
                any()
        )).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });

        OrderService paymentOrderService = new OrderService(
                orderRepository,
                productClient,
                paymentClient,
                circuitBreakerFactory,
                Retry.ofDefaults("paymentService"),
                orderRateLimiter,
                Bulkhead.ofDefaults("paymentService"),
                orderPersistenceService
        );

        paymentOrderService.createPayment(request);

        verify(circuitBreakerFactory).create("paymentService");
        verify(circuitBreaker).run(any(Supplier.class), any());
        verify(paymentClient).createPayment(request);
    }
    @Test
    void createPaymentRetriesAfterFailure() {
        PaymentRequest request = new PaymentRequest(
                1L, 10L, 2499.0, "CARD"
        );

        CircuitBreaker circuitBreaker = mock(CircuitBreaker.class);

        when(circuitBreakerFactory.create("paymentService"))
                .thenReturn(circuitBreaker);

        when(circuitBreaker.run(any(Supplier.class), any()))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(0);
                    return supplier.get();
                });

        AtomicInteger attempts = new AtomicInteger(0);

        when(paymentClient.createPayment(request))
                .thenAnswer(invocation -> {
                    if (attempts.incrementAndGet() < 3) {
                        throw new RuntimeException("Payment service unavailable");
                    }
                    return new Object();
                });

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ZERO)
                .build();

        OrderService paymentOrderService = new OrderService(
                orderRepository,
                productClient,
                paymentClient,
                circuitBreakerFactory,
                Retry.of("paymentService", config),
                orderRateLimiter,
                Bulkhead.ofDefaults("paymentService"),
                orderPersistenceService
        );

        paymentOrderService.createPayment(request);

        assertEquals(3, attempts.get());
        verify(paymentClient, times(3)).createPayment(request);
    }
    @Test
    void createPaymentThrowsExceptionWhenCircuitBreakerFails() {
        PaymentRequest request = new PaymentRequest(
                1L, 10L, 2499.0, "CARD"
        );

        CircuitBreaker circuitBreaker = mock(CircuitBreaker.class);

        when(circuitBreakerFactory.create("paymentService"))
                .thenReturn(circuitBreaker);

        when(circuitBreaker.run(any(Supplier.class), any()))
                .thenAnswer(invocation -> {
                    Function<Throwable, ?> fallback = invocation.getArgument(1);
                    return fallback.apply(
                            new RuntimeException("Payment service unavailable")
                    );
                });

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(1)
                .waitDuration(Duration.ZERO)
                .build();

        OrderService paymentOrderService = new OrderService(
                orderRepository,
                productClient,
                paymentClient,
                circuitBreakerFactory,
                Retry.of("paymentService", config),
                orderRateLimiter,
                Bulkhead.ofDefaults("paymentService"),
                orderPersistenceService
        );

        PaymentServiceException exception = assertThrows(
                PaymentServiceException.class,
                () -> paymentOrderService.createPayment(request)
        );

        assertEquals(
                "Payment service is unavailable. Order cannot be processed.",
                exception.getMessage()
        );

        verify(paymentClient, never()).createPayment(request);
    }
    @Test
    void createPaymentRejectsRequestWhenBulkheadIsFull() {
        PaymentRequest request = new PaymentRequest(
                1L, 10L, 2499.0, "CARD"
        );

        CircuitBreaker circuitBreaker = mock(CircuitBreaker.class);

        when(circuitBreakerFactory.create("paymentService"))
                .thenReturn(circuitBreaker);

        Bulkhead paymentBulkhead = Bulkhead.of(
                "paymentService",
                BulkheadConfig.custom()
                        .maxConcurrentCalls(1)
                        .maxWaitDuration(Duration.ZERO)
                        .build()
        );

        paymentBulkhead.tryAcquirePermission();

        OrderService paymentOrderService = new OrderService(
                orderRepository,
                productClient,
                paymentClient,
                circuitBreakerFactory,
                Retry.of("paymentService", RetryConfig.custom()
                        .maxAttempts(1)
                        .build()),
                orderRateLimiter,
                paymentBulkhead,
                orderPersistenceService
        );

        assertThrows(
                io.github.resilience4j.bulkhead.BulkheadFullException.class,
                () -> paymentOrderService.createPayment(request)
        );

        paymentBulkhead.releasePermission();

        verify(circuitBreaker, never()).run(any(Supplier.class), any());
        verify(paymentClient, never()).createPayment(request);
    }
}
