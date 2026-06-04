package com.scarlxrd.payment_service.outbox;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED
    }
