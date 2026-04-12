package com.scarlxrd.payment_service.service;

import com.scarlxrd.payment_service.dto.OrderCreatedEvent;
import com.scarlxrd.payment_service.dto.PaymentRequestDTO;
import com.scarlxrd.payment_service.dto.PaymentResponseDTO;
import com.scarlxrd.payment_service.dto.PaymentResultEvent;
import com.scarlxrd.payment_service.entity.Payment;
import com.scarlxrd.payment_service.exception.PaymentException;
import com.scarlxrd.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@RequiredArgsConstructor
@Service
public class PaymentService {

    private final PaymentRepository repository;
    private final Random random = new Random();

    @Transactional
    public PaymentResultEvent process(OrderCreatedEvent event) {

        int result = random.nextInt(100);

        Payment payment = new Payment();
        payment.setOrderId(event.getOrderId());
        payment.setAmount(event.getAmount());

        if (result <= 70) {
            payment.setStatus("SUCCESS");
            repository.save(payment);

            return new PaymentResultEvent(
                    event.getOrderId(),
                    "SUCCESS",
                    event.getAmount()
            );
        }

        if (result <= 90) {
            payment.setStatus("FAILED");
            repository.save(payment);

            return new PaymentResultEvent(
                    event.getOrderId(),
                    "FAILED",
                    event.getAmount()
            );
        }

        throw new PaymentException("Serviço de pagamento indisponível");
    }
}
