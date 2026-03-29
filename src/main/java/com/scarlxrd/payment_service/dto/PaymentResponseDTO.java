package com.scarlxrd.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class PaymentResponseDTO {

    private String status;
    private String message;

}
