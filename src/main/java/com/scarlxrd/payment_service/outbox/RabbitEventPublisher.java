package com.scarlxrd.payment_service.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    private static final String EXCHANGE = "book.events";

    @Override
    public void publish(OutboxEvent event) {
        try {
            JsonNode payload = objectMapper.readTree(event.getPayload());

            rabbitTemplate.convertAndSend(
                    EXCHANGE,
                    resolveRoutingKey(event),
                    payload
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to publish outbox event", e);
        }
    }

    private String resolveRoutingKey(OutboxEvent event) {

        if ("PAYMENT_APPROVED".equals(event.getEventType())) {
            return "payment.result.success";
        }

        if ("PAYMENT_FAILED".equals(event.getEventType())) {
            return "payment.result.failed";
        }

        throw new IllegalArgumentException(
                "Routing key não configurada: " + event.getEventType()
        );
    }
}