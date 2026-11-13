
package com.Ecommerce.payment_service.service;

import com.Ecommerce.payment_service.dto.PaymentRequest;
import com.Ecommerce.payment_service.entity.Payment;
import com.Ecommerce.payment_service.entity.PaymentMethod;
import com.Ecommerce.payment_service.entity.PaymentStatus;
import com.Ecommerce.payment_service.event.PaymentFailedEvent;
import com.Ecommerce.payment_service.event.PaymentSucceededEvent;
import com.Ecommerce.payment_service.kafka.KafkaProducerService;
import com.Ecommerce.payment_service.repository.PaymentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaProducerService kafkaProducerService;

    public PaymentService(
            PaymentRepository paymentRepository,
            KafkaProducerService kafkaProducerService) {
        this.paymentRepository = paymentRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    public Payment createPayment(PaymentRequest request) {

        var existingPayment =
                paymentRepository.findByOrderId(request.orderId());

        if (existingPayment.isPresent()) {
            return existingPayment.get();
        }

        Payment payment = Payment.builder()
                .orderId(request.orderId())
                .userId(request.userId())
                .amount(request.amount())
                .paymentMethod(request.paymentMethod())
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            return paymentRepository.saveAndFlush(payment);

        } catch (DataIntegrityViolationException e) {

            return paymentRepository.findByOrderId(request.orderId())
                    .orElseThrow(() -> e);
        }
    }

    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found"
                ));
    }

    public Payment collectCodPayment(Long orderId) {

        Payment payment = getPaymentByOrderId(orderId);

        if (payment.getPaymentMethod() != PaymentMethod.COD) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This order is not a Cash on Delivery order"
            );
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "COD payment is not pending. Current status: "
                            + payment.getStatus()
            );
        }

        payment.setStatus(PaymentStatus.SUCCESS);

        return paymentRepository.save(payment);
    }

    public Payment updatePaymentStatus(Long id, PaymentStatus status) {

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found"
                ));

        if (payment.getStatus() != PaymentStatus.PENDING
                && payment.getStatus() != status) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot change payment status from "
                            + payment.getStatus() + " to " + status
            );
        }

        boolean newlySuccessful =
                status == PaymentStatus.SUCCESS
                        && payment.getStatus() != PaymentStatus.SUCCESS;

        boolean newlyFailed =
                status == PaymentStatus.FAILED
                        && payment.getStatus() != PaymentStatus.FAILED;

        payment.setStatus(status);
        Payment savedPayment = paymentRepository.save(payment);

        if (newlySuccessful) {
            PaymentSucceededEvent event = new PaymentSucceededEvent(
                    savedPayment.getId(),
                    savedPayment.getOrderId(),
                    savedPayment.getUserId(),
                    savedPayment.getAmount()
            );

            kafkaProducerService.publishPaymentSucceeded(event);
        }

        if (newlyFailed) {
            PaymentFailedEvent event = new PaymentFailedEvent(
                    savedPayment.getId(),
                    savedPayment.getOrderId(),
                    savedPayment.getUserId(),
                    savedPayment.getAmount()
            );

            kafkaProducerService.publishPaymentFailed(event);
        }

        return savedPayment;
    }
}
