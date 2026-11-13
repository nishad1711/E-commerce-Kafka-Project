package com.Ecommerce.payment_service.controller;

import com.Ecommerce.payment_service.dto.PaymentRequest;
import com.Ecommerce.payment_service.entity.Payment;
import com.Ecommerce.payment_service.entity.PaymentStatus;
import com.Ecommerce.payment_service.security.UserPrincipal;
import com.Ecommerce.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Payment> createPayment(
            @Valid @RequestBody PaymentRequest request) {

        return ResponseEntity.ok(
                paymentService.createPayment(request)
        );
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Payment> getPaymentByOrderId(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal) {

        Payment payment = paymentService.getPaymentByOrderId(orderId);

        if (!"ADMIN".equals(principal.role())
                && !payment.getUserId().equals(principal.userId())) {
            throw new AccessDeniedException(
                    "You cannot access another user's payment."
            );
        }

        return ResponseEntity.ok(payment);
    }
    @PutMapping("/order/{orderId}/cod/collect")
    public ResponseEntity<Payment> collectCodPayment(
            @PathVariable Long orderId) {

        return ResponseEntity.ok(
                paymentService.collectCodPayment(orderId)
        );
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Payment> updatePaymentStatus(
            @PathVariable Long id,
            @RequestParam PaymentStatus status) {

        return ResponseEntity.ok(
                paymentService.updatePaymentStatus(id, status)
        );
    }
}
