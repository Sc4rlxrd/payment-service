package com.scarlxrd.payment_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class PaymentRequestDTO {

    @NotNull
    private UUID orderId;

    @NotNull
    private BigDecimal amount;


}
