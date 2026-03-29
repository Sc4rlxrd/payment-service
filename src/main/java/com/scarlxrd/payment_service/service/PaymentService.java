package com.scarlxrd.payment_service.service;

import com.scarlxrd.payment_service.dto.PaymentRequestDTO;
import com.scarlxrd.payment_service.dto.PaymentResponseDTO;
import com.scarlxrd.payment_service.entity.Payment;
import com.scarlxrd.payment_service.exception.PaymentException;
import com.scarlxrd.payment_service.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
public class PaymentService {

    private final PaymentRepository repository;
    private final Random random = new Random();

    public PaymentService(PaymentRepository repository) {
        this.repository = repository;
    }
    @Transactional
    public PaymentResponseDTO process(PaymentRequestDTO request) {

        int result = random.nextInt(100);

        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());

        if (result <= 70) {
            payment.setStatus("SUCCESS");
            repository.save(payment);
            return new PaymentResponseDTO("SUCCESS", "Pagamento aprovado");
        }

        if (result <= 90) {
            payment.setStatus("FAILED");
            repository.save(payment);
            return new PaymentResponseDTO("FAILED", "Pagamento recusado");
        }

        throw new PaymentException("Serviço de pagamento indisponível");
    }

}
