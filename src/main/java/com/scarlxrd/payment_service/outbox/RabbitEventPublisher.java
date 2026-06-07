package com.scarlxrd.payment_service.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scarlxrd.payment_service.config.metrics.RabbitEventMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final RabbitEventMetrics rabbitMetrics;
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
            rabbitMetrics.published("payment_result");

        } catch (Exception e) {
            rabbitMetrics.failed("payment_result");
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