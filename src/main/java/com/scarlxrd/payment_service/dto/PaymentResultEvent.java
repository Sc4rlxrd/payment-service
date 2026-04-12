package com.scarlxrd.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResultEvent {

    private UUID orderId;
    private String status;
    private BigDecimal amount;

    public PaymentResultEvent(UUID orderId, String status) {
        this.orderId = orderId;
        this.status = status;
    }
}