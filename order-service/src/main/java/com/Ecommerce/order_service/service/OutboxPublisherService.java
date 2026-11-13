
package com.Ecommerce.order_service.service;

import com.Ecommerce.order_service.entity.OutboxEvent;
import com.Ecommerce.order_service.event.OrderCreatedEvent;
import com.Ecommerce.order_service.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OutboxPublisherService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisherService(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {

        List<OutboxEvent> events =
                outboxEventRepository
                        .findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();

        for (OutboxEvent outboxEvent : events) {

            try {
                OrderCreatedEvent event =
                        objectMapper.readValue(
                                outboxEvent.getPayload(),
                                OrderCreatedEvent.class
                        );

                kafkaTemplate.send(
                        "order-created",
                        event.orderId().toString(),
                        event
                ).get();

                outboxEvent.setPublishedAt(LocalDateTime.now());

                outboxEventRepository.save(outboxEvent);

                System.out.println(
                        "Outbox event published: "
                                + outboxEvent.getId()
                );

            } catch (Exception e) {
                System.err.println(
                        "Failed to publish outbox event: "
                                + outboxEvent.getId()
                );
                e.printStackTrace();
            }
        }
    }
}
