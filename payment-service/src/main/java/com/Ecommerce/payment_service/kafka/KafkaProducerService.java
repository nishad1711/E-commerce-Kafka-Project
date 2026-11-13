package com.Ecommerce.payment_service.kafka;

import com.Ecommerce.payment_service.event.PaymentFailedEvent;
import com.Ecommerce.payment_service.event.PaymentSucceededEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, PaymentSucceededEvent> successTemplate;
    private final KafkaTemplate<String, PaymentFailedEvent> failureTemplate;

    public KafkaProducerService(
            KafkaTemplate<String, PaymentSucceededEvent> successTemplate,
            KafkaTemplate<String, PaymentFailedEvent> failureTemplate
    ) {
        this.successTemplate = successTemplate;
        this.failureTemplate = failureTemplate;
    }

    public void publishPaymentSucceeded(PaymentSucceededEvent event) {
        successTemplate.send(
                "payment-success",
                event.orderId().toString(),
                event
        );
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        failureTemplate.send(
                "payment-failed",
                event.orderId().toString(),
                event
        );
    }
}
