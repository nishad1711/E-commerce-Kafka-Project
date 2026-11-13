package com.Ecommerce.payment_service.controller;

import com.Ecommerce.payment_service.dto.PaymentRequest;
import com.Ecommerce.payment_service.entity.Payment;
import com.Ecommerce.payment_service.entity.PaymentMethod;
import com.Ecommerce.payment_service.entity.PaymentStatus;
import com.Ecommerce.payment_service.service.PaymentService;
import com.Ecommerce.payment_service.security.UserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PaymentController controller = new PaymentController(paymentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private Payment payment() {
        return Payment.builder()
                .id(100L)
                .orderId(1L)
                .userId(10L)
                .amount(2500.0)
                .paymentMethod(PaymentMethod.UPI)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createPaymentShouldReturnOk() throws Exception {
        when(paymentService.createPayment(any(PaymentRequest.class)))
                .thenReturn(payment());

        mockMvc.perform(post("/payments")
                        .contentType("application/json")
                        .content("""
                                {
                                    "orderId": 1,
                                    "userId": 10,
                                    "amount": 2500.0,
                                    "paymentMethod": "UPI"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(paymentService).createPayment(any(PaymentRequest.class));
    }

    @Test
    void createPaymentShouldRejectMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/payments")
                        .contentType("application/json")
                        .content("""
                                {
                                    "userId": 10,
                                    "amount": 2500.0,
                                    "paymentMethod": "UPI"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    void createPaymentShouldRejectAmountBelowMinimum() throws Exception {
        mockMvc.perform(post("/payments")
                        .contentType("application/json")
                        .content("""
                                {
                                    "orderId": 1,
                                    "userId": 10,
                                    "amount": 0,
                                    "paymentMethod": "UPI"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    void getPaymentByOrderIdShouldReturnOk() {
        when(paymentService.getPaymentByOrderId(1L)).thenReturn(payment());

        PaymentController controller = new PaymentController(paymentService);

        var response = controller.getPaymentByOrderId(
                1L,
                new UserPrincipal(10L, "test@example.com", "USER")
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                200, response.getStatusCode().value()
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                1L, response.getBody().getOrderId()
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                2500.0, response.getBody().getAmount()
        );

        verify(paymentService).getPaymentByOrderId(1L);
    }

    @Test
    void updatePaymentStatusShouldReturnOk() throws Exception {
        Payment updatedPayment = payment();
        updatedPayment.setStatus(PaymentStatus.SUCCESS);

        when(paymentService.updatePaymentStatus(100L, PaymentStatus.SUCCESS))
                .thenReturn(updatedPayment);

        mockMvc.perform(put("/payments/100/status")
                        .param("status", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(paymentService).updatePaymentStatus(100L, PaymentStatus.SUCCESS);
    }
}
