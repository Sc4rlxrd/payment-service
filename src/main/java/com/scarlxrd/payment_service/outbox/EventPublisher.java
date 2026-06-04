package com.scarlxrd.payment_service.outbox;

public interface EventPublisher {
    void publish(OutboxEvent event);
}