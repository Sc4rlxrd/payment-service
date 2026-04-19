package com.scarlxrd.payment_service.service;

import com.scarlxrd.payment_service.dto.OrderCreatedEvent;
import com.scarlxrd.payment_service.dto.PaymentRequestDTO;
import com.scarlxrd.payment_service.dto.PaymentResponseDTO;
import com.scarlxrd.payment_service.dto.PaymentResultEvent;
import com.scarlxrd.payment_service.entity.Payment;
import com.scarlxrd.payment_service.entity.PaymentStatus;
import com.scarlxrd.payment_service.exception.PaymentException;
import com.scarlxrd.payment_service.impl.PaymentProcessor;
import com.scarlxrd.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@RequiredArgsConstructor
@Service
public class PaymentService {

    private final PaymentRepository repository;
    private final PaymentProcessor processor;

    @Transactional
    public PaymentResultEvent process(PaymentRequestDTO  event) {

        PaymentStatus status = processor.process();

        Payment payment = new Payment();
        payment.setOrderId(event.getOrderId());
        payment.setAmount(event.getAmount());
        payment.setStatus(status);

        repository.save(payment);

        if (status == PaymentStatus.UNAVAILABLE) {
            throw new PaymentException("Serviço de pagamento indisponível");
        }

        return new PaymentResultEvent(
                event.getOrderId(),
                status.name(),
                event.getAmount()
        );
    }
}