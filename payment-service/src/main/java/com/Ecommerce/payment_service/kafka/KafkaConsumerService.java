package com.Ecommerce.payment_service.kafka;

import com.Ecommerce.payment_service.dto.PaymentRequest;
import com.Ecommerce.payment_service.entity.Payment;
import com.Ecommerce.payment_service.entity.PaymentMethod;
import com.Ecommerce.payment_service.event.OrderCreatedEvent;
import com.Ecommerce.payment_service.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    private final PaymentService paymentService;

    public KafkaConsumerService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "order-created", groupId = "payment-service")
    public void consumeOrderCreated(OrderCreatedEvent event) {

        System.out.println("Order created event received!");
        System.out.println("Order ID: " + event.orderId());
        System.out.println("User ID: " + event.userId());
        System.out.println("Amount: " + event.totalAmount());

        PaymentRequest request = new PaymentRequest(
                event.orderId(),
                event.userId(),
                event.totalAmount(),
                PaymentMethod.COD
        );

        Payment payment = paymentService.createPayment(request);

        System.out.println(
                "COD payment created for order "
                        + event.orderId()
                        + " with status: "
                        + payment.getStatus()
        );
    }
}
