
package com.Ecommerce.order_service.kafka;

import com.Ecommerce.order_service.client.ProductClient;
import com.Ecommerce.order_service.entity.Order;
import com.Ecommerce.order_service.entity.OrderItem;
import com.Ecommerce.order_service.entity.OrderStatus;
import com.Ecommerce.order_service.event.PaymentFailedEvent;
import com.Ecommerce.order_service.event.PaymentSucceededEvent;
import com.Ecommerce.order_service.repository.OrderRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KafkaConsumerService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;

    public KafkaConsumerService(
            OrderRepository orderRepository,
            ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
    }

    @KafkaListener(topics = "payment-success", groupId = "order-service")
    @Transactional
    public void consumePaymentSucceeded(PaymentSucceededEvent event) {
        System.out.println("Payment success event received!");
        System.out.println("Order ID: " + event.orderId());

        orderRepository.findById(event.orderId())
                .ifPresent(order -> {
                    if (order.getStatus() == OrderStatus.CREATED) {
                        order.setStatus(OrderStatus.PAID);
                        orderRepository.save(order);
                        System.out.println(
                                "Order " + event.orderId() + " marked as PAID"
                        );
                    }
                });
    }

    @KafkaListener(topics = "payment-failed", groupId = "order-service")
    @Transactional
    public void consumePaymentFailed(PaymentFailedEvent event) {
        System.out.println("Payment failure event received!");
        System.out.println("Order ID: " + event.orderId());

        orderRepository.findById(event.orderId())
                .ifPresent(order -> {
                    if (order.getStatus() != OrderStatus.CREATED) {
                        System.out.println(
                                "Ignoring failure event because order status is "
                                        + order.getStatus()
                        );
                        return;
                    }

                    order.setStatus(OrderStatus.CANCELLED);
                    orderRepository.save(order);

                    for (OrderItem item : order.getItems()) {
                        String operationKey =
                                "ORDER-" + event.orderId() + "-ITEM-" + item.getId();

                        productClient.releaseStock(
                                item.getProductId(),
                                item.getQuantity(),
                                operationKey
                        );
                    }

                    System.out.println(
                            "Order " + event.orderId()
                                    + " cancelled and reserved stock released"
                    );
                });
    }
}
