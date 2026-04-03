package com.scarlxrd.payment_service.controller;

import com.scarlxrd.payment_service.dto.PaymentRequestDTO;
import com.scarlxrd.payment_service.dto.PaymentResponseDTO;
import com.scarlxrd.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }
//
//    @PostMapping
//    public PaymentResponseDTO process(@RequestBody @Valid PaymentRequestDTO dto) {
//        return service.process(dto);
//    }
}