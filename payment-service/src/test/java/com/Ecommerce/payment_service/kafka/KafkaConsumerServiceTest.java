package com.Ecommerce.payment_service.kafka;

import com.Ecommerce.payment_service.dto.PaymentRequest;
import com.Ecommerce.payment_service.entity.Payment;
import com.Ecommerce.payment_service.entity.PaymentMethod;
import com.Ecommerce.payment_service.entity.PaymentStatus;
import com.Ecommerce.payment_service.event.OrderCreatedEvent;
import com.Ecommerce.payment_service.event.OrderItemEvent;
import com.Ecommerce.payment_service.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerServiceTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private KafkaConsumerService kafkaConsumerService;

    @Test
    void consumeOrderCreatedShouldCreateAndMarkPaymentSuccessful() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                1L,
                10L,
                2500.0,
                List.of(new OrderItemEvent(5L, 2, 1250.0))
        );

        Payment pendingPayment = Payment.builder()
                .id(100L)
                .orderId(1L)
                .userId(10L)
                .amount(2500.0)
                .paymentMethod(PaymentMethod.UPI)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Payment successfulPayment = Payment.builder()
                .id(100L)
                .orderId(1L)
                .userId(10L)
                .amount(2500.0)
                .paymentMethod(PaymentMethod.UPI)
                .status(PaymentStatus.SUCCESS)
                .createdAt(pendingPayment.getCreatedAt())
                .build();

        when(paymentService.createPayment(any(PaymentRequest.class)))
                .thenReturn(pendingPayment);

        when(paymentService.updatePaymentStatus(100L, PaymentStatus.SUCCESS))
                .thenReturn(successfulPayment);

        kafkaConsumerService.consumeOrderCreated(event);

        verify(paymentService).createPayment(argThat(request ->
                request.orderId().equals(1L)
                        && request.userId().equals(10L)
                        && request.amount().equals(2500.0)
                        && request.paymentMethod() == PaymentMethod.UPI
        ));

        verify(paymentService).updatePaymentStatus(
                100L, PaymentStatus.SUCCESS
        );
    }

    @Test
    void consumeOrderCreatedShouldPropagatePaymentCreationFailure() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                2L,
                20L,
                1000.0,
                List.of()
        );

        when(paymentService.createPayment(any(PaymentRequest.class)))
                .thenThrow(new RuntimeException("Payment creation failed"));

        assertThrows(
                RuntimeException.class,
                () -> kafkaConsumerService.consumeOrderCreated(event)
        );

        verify(paymentService, never())
                .updatePaymentStatus(anyLong(), any());
    }

    @Test
    void consumeOrderCreatedShouldPropagateStatusUpdateFailure() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                3L,
                30L,
                1500.0,
                List.of()
        );

        Payment pendingPayment = Payment.builder()
                .id(300L)
                .orderId(3L)
                .userId(30L)
                .amount(1500.0)
                .paymentMethod(PaymentMethod.UPI)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentService.createPayment(any(PaymentRequest.class)))
                .thenReturn(pendingPayment);

        when(paymentService.updatePaymentStatus(300L, PaymentStatus.SUCCESS))
                .thenThrow(new RuntimeException("Status update failed"));

        assertThrows(
                RuntimeException.class,
                () -> kafkaConsumerService.consumeOrderCreated(event)
        );

        verify(paymentService).updatePaymentStatus(
                300L, PaymentStatus.SUCCESS
        );
    }
}
