package com.scarlxrd.payment_service.service;

import com.scarlxrd.payment_service.dto.PaymentRequestDTO;
import com.scarlxrd.payment_service.dto.PaymentResultEvent;
import com.scarlxrd.payment_service.entity.Payment;
import com.scarlxrd.payment_service.entity.PaymentStatus;
import com.scarlxrd.payment_service.exception.PaymentException;
import com.scarlxrd.payment_service.impl.PaymentProcessor;
import com.scarlxrd.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class PaymentService {

    private final PaymentRepository repository;
    private final PaymentProcessor processor;

    @Transactional
    public PaymentResultEvent process(PaymentRequestDTO event) {

        log.info("Initiating payment processing | orderId={}   amount={}", event.getOrderId(), event.getAmount());

        long start = System.currentTimeMillis();

        try {

            UUID eventId = UUID.randomUUID();
            var existing = repository.findByOrderId(event.getOrderId());

            if (existing.isPresent()) {
                Payment payment = existing.get();

                log.warn("Payment already processed | orderId={} status={}", payment.getOrderId(), payment.getStatus());

                return new PaymentResultEvent(
                        eventId,
                        payment.getOrderId(),
                        payment.getStatus().name(),
                        payment.getAmount()
                );
            }

            PaymentStatus status = processor.process();

            log.info("Processing result | orderId={} status={}", event.getOrderId(), status);

            if (status == PaymentStatus.UNAVAILABLE) {
                PaymentException ex = new PaymentException("Payment service unavailable");
                log.error("Payment service unavailable | orderId={}", event.getOrderId(), ex);
                throw ex;
            }

            Payment payment = Payment.builder()
                    .orderId(event.getOrderId())
                    .amount(event.getAmount())
                    .status(status)
                    .build();

            repository.save(payment);

            log.info("Payment processed successfully | orderId={} status={}",
                    event.getOrderId(),
                    status);


            return new PaymentResultEvent(
                    eventId,
                    payment.getOrderId(),
                    payment.getStatus().name(),
                    payment.getAmount()
            );

        } catch (DataIntegrityViolationException ex) {
            log.warn("Duplicate detected via DB constraint | orderId={}", event.getOrderId());

            Payment existingPayment = repository.findByOrderId(event.getOrderId()).orElseThrow();
            return new PaymentResultEvent(
                   UUID.randomUUID(),
                    existingPayment.getOrderId(),
                    existingPayment.getStatus().name(),
                    existingPayment.getAmount());

        }finally {
            log.info("Finished processing | orderId={} duration={}ms",
                    event.getOrderId(),
                    System.currentTimeMillis() - start);
        }
    }
}