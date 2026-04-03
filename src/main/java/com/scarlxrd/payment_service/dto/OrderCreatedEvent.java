package com.scarlxrd.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreatedEvent {

    private UUID orderId;
    private BigDecimal amount;
    private String customerEmail;
}
