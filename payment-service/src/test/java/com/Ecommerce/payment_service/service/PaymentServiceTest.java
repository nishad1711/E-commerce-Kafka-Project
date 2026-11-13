package com.Ecommerce.payment_service.service;

import com.Ecommerce.payment_service.dto.PaymentRequest;
import com.Ecommerce.payment_service.entity.Payment;
import com.Ecommerce.payment_service.entity.PaymentMethod;
import com.Ecommerce.payment_service.entity.PaymentStatus;
import com.Ecommerce.payment_service.event.PaymentFailedEvent;
import com.Ecommerce.payment_service.event.PaymentSucceededEvent;
import com.Ecommerce.payment_service.kafka.KafkaProducerService;
import com.Ecommerce.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequest request() {
        return new PaymentRequest(1L, 10L, 2500.0, PaymentMethod.UPI);
    }

    private Payment payment(PaymentStatus status) {
        return Payment.builder()
                .id(100L)
                .orderId(1L)
                .userId(10L)
                .amount(2500.0)
                .paymentMethod(PaymentMethod.UPI)
                .status(status)
                .createdAt(java.time.LocalDateTime.now())
                .build();
    }

    @Test
    void createPaymentShouldCreatePendingPayment() {
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(paymentRepository.saveAndFlush(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.createPayment(request());

        assertEquals(1L, result.getOrderId());
        assertEquals(10L, result.getUserId());
        assertEquals(2500.0, result.getAmount());
        assertEquals(PaymentMethod.UPI, result.getPaymentMethod());
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertNotNull(result.getCreatedAt());

        verify(paymentRepository).saveAndFlush(any(Payment.class));
    }

    @Test
    void createPaymentShouldReturnExistingPayment() {
        Payment existing = payment(PaymentStatus.SUCCESS);

        when(paymentRepository.findByOrderId(1L))
                .thenReturn(Optional.of(existing));

        Payment result = paymentService.createPayment(request());

        assertSame(existing, result);
        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    void createPaymentShouldHandleDuplicateDatabaseInsert() {
        Payment existing = payment(PaymentStatus.PENDING);

        when(paymentRepository.findByOrderId(1L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));

        when(paymentRepository.saveAndFlush(any(Payment.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate order"));

        Payment result = paymentService.createPayment(request());

        assertSame(existing, result);
        verify(paymentRepository, times(2)).findByOrderId(1L);
    }

    @Test
    void createPaymentShouldRethrowDatabaseExceptionWhenDuplicateIsNotFound() {
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException("Database error");

        when(paymentRepository.findByOrderId(1L))
                .thenReturn(Optional.empty());

        when(paymentRepository.saveAndFlush(any(Payment.class)))
                .thenThrow(exception);

        assertSame(exception, assertThrows(
                DataIntegrityViolationException.class,
                () -> paymentService.createPayment(request())
        ));
    }

    @Test
    void getPaymentByOrderIdShouldReturnPayment() {
        Payment existing = payment(PaymentStatus.PENDING);

        when(paymentRepository.findByOrderId(1L))
                .thenReturn(Optional.of(existing));

        assertSame(existing, paymentService.getPaymentByOrderId(1L));
    }

    @Test
    void getPaymentByOrderIdShouldThrowWhenNotFound() {
        when(paymentRepository.findByOrderId(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> paymentService.getPaymentByOrderId(1L)
        );
    }

    @Test
    void updatePaymentStatusShouldPublishSuccessEvent() {
        Payment existing = payment(PaymentStatus.PENDING);

        when(paymentRepository.findById(100L))
                .thenReturn(Optional.of(existing));
        when(paymentRepository.save(existing)).thenReturn(existing);

        Payment result = paymentService.updatePaymentStatus(
                100L, PaymentStatus.SUCCESS
        );

        assertEquals(PaymentStatus.SUCCESS, result.getStatus());

        verify(kafkaProducerService)
                .publishPaymentSucceeded(any(PaymentSucceededEvent.class));
        verify(kafkaProducerService, never())
                .publishPaymentFailed(any(PaymentFailedEvent.class));
    }

    @Test
    void updatePaymentStatusShouldPublishFailureEvent() {
        Payment existing = payment(PaymentStatus.PENDING);

        when(paymentRepository.findById(100L))
                .thenReturn(Optional.of(existing));
        when(paymentRepository.save(existing)).thenReturn(existing);

        Payment result = paymentService.updatePaymentStatus(
                100L, PaymentStatus.FAILED
        );

        assertEquals(PaymentStatus.FAILED, result.getStatus());

        verify(kafkaProducerService)
                .publishPaymentFailed(any(PaymentFailedEvent.class));
        verify(kafkaProducerService, never())
                .publishPaymentSucceeded(any(PaymentSucceededEvent.class));
    }

    @Test
    void updatePaymentStatusShouldNotPublishEventForRepeatedSuccess() {
        Payment existing = payment(PaymentStatus.SUCCESS);

        when(paymentRepository.findById(100L))
                .thenReturn(Optional.of(existing));
        when(paymentRepository.save(existing)).thenReturn(existing);

        paymentService.updatePaymentStatus(100L, PaymentStatus.SUCCESS);

        verify(kafkaProducerService, never())
                .publishPaymentSucceeded(any(PaymentSucceededEvent.class));
        verify(kafkaProducerService, never())
                .publishPaymentFailed(any(PaymentFailedEvent.class));
    }

    @Test
    void updatePaymentStatusShouldNotPublishEventForRepeatedFailure() {
        Payment existing = payment(PaymentStatus.FAILED);

        when(paymentRepository.findById(100L))
                .thenReturn(Optional.of(existing));
        when(paymentRepository.save(existing)).thenReturn(existing);

        paymentService.updatePaymentStatus(100L, PaymentStatus.FAILED);

        verify(kafkaProducerService, never())
                .publishPaymentSucceeded(any(PaymentSucceededEvent.class));
        verify(kafkaProducerService, never())
                .publishPaymentFailed(any(PaymentFailedEvent.class));
    }

    @Test
    void updatePaymentStatusShouldRejectChangingTerminalStatus() {
        Payment existing = payment(PaymentStatus.SUCCESS);

        when(paymentRepository.findById(100L))
                .thenReturn(Optional.of(existing));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> paymentService.updatePaymentStatus(
                        100L, PaymentStatus.FAILED
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(kafkaProducerService);
    }

    @Test
    void updatePaymentStatusShouldThrowWhenPaymentDoesNotExist() {
        when(paymentRepository.findById(100L))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> paymentService.updatePaymentStatus(
                        100L, PaymentStatus.SUCCESS
                )
        );

        verifyNoInteractions(kafkaProducerService);
    }

    @Test
    void pendingPaymentShouldAllowRefundedStatusAccordingToCurrentLogic() {
        Payment existing = payment(PaymentStatus.PENDING);

        when(paymentRepository.findById(100L))
                .thenReturn(Optional.of(existing));
        when(paymentRepository.save(existing)).thenReturn(existing);

        Payment result = paymentService.updatePaymentStatus(
                100L, PaymentStatus.REFUNDED
        );

        assertEquals(PaymentStatus.REFUNDED, result.getStatus());
        verifyNoInteractions(kafkaProducerService);
    }
}
