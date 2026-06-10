package com.scarlxrd.payment_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scarlxrd.payment_service.config.metrics.OutboxMetrics;
import com.scarlxrd.payment_service.config.metrics.PaymentMetrics;
import com.scarlxrd.payment_service.config.metrics.RabbitEventMetrics;
import com.scarlxrd.payment_service.dto.PaymentRequestDTO;
import com.scarlxrd.payment_service.dto.PaymentResultEvent;
import com.scarlxrd.payment_service.entity.Payment;
import com.scarlxrd.payment_service.entity.PaymentStatus;
import com.scarlxrd.payment_service.exception.PaymentException;
import com.scarlxrd.payment_service.impl.PaymentProcessor;
import com.scarlxrd.payment_service.outbox.OutboxEvent;
import com.scarlxrd.payment_service.outbox.OutboxRepository;
import com.scarlxrd.payment_service.outbox.OutboxStatus;
import com.scarlxrd.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class PaymentService {

    private final PaymentRepository repository;
    private final PaymentProcessor processor;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final PaymentMetrics paymentMetrics;
    private final OutboxMetrics outboxMetrics;
    private final RabbitEventMetrics rabbitMetrics;

    @Transactional
    public PaymentResultEvent process(PaymentRequestDTO event) {

        log.info("Initiating payment processing | orderId={}   amount={}", event.getOrderId(), event.getAmount());

        long start = System.currentTimeMillis();

        try {

            UUID eventId = UUID.randomUUID();
            var existing = repository.findByOrderId(event.getOrderId());

            if (existing.isPresent()) {
                Payment payment = existing.get();

                rabbitMetrics.duplicated("payment_process");

                log.warn("Payment already processed | orderId={} status={}", payment.getOrderId(), payment.getStatus());

                return new PaymentResultEvent(
                        eventId,
                        payment.getOrderId(),
                        payment.getStatus().name(),
                        payment.getAmount()
                );
            }

            PaymentStatus status = processor.process();

            paymentMetrics.processed();

            log.info("Processing result | orderId={} status={}", event.getOrderId(), status);

            if (status == PaymentStatus.UNAVAILABLE) {
                paymentMetrics.paymentError("service_unavailable");
                PaymentException ex = new PaymentException("Payment service unavailable");
                log.error("Payment service unavailable | orderId={}", event.getOrderId(), ex);
                throw ex;
            }

            Payment payment = Payment.builder()
                    .orderId(event.getOrderId())
                    .amount(event.getAmount())
                    .status(status)
                    .build();

            Payment savedPayment = repository.saveAndFlush(payment);

            if (savedPayment.getStatus() == PaymentStatus.SUCCESS) {
                paymentMetrics.paymentSuccess();
            } else if (savedPayment.getStatus() == PaymentStatus.FAILED) {
                paymentMetrics.paymentError("payment_failed");
            }

            log.info("Payment processed successfully | orderId={} status={}",
                    event.getOrderId(),
                    status);


            PaymentResultEvent resultEvent = new PaymentResultEvent(
                    eventId,
                    savedPayment.getOrderId(),
                    savedPayment.getStatus().name(),
                    savedPayment.getAmount()
            );

            savePaymentResultOutboxEvent(savedPayment, resultEvent);

            return resultEvent;

        } catch (DataIntegrityViolationException ex) {
            rabbitMetrics.duplicated("payment_process");
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

    private void savePaymentResultOutboxEvent(
            Payment payment,
            PaymentResultEvent resultEvent
    ) {
        try {
            String eventType;

            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                eventType = "PAYMENT_APPROVED";
            } else if (payment.getStatus() == PaymentStatus.FAILED) {
                eventType = "PAYMENT_FAILED";
            } else {
                log.warn(
                        "Ignoring payment status for outbox | orderId={} status={}",
                        payment.getOrderId(),
                        payment.getStatus()
                );
                return;
            }

            String payload = objectMapper.writeValueAsString(resultEvent);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(resultEvent.getEventId())
                    .aggregateId(payment.getOrderId())
                    .aggregateType("PAYMENT")
                    .eventType(eventType)
                    .payload(payload)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxRepository.save(outboxEvent);

            outboxMetrics.created();

        } catch (JsonProcessingException e) {
            outboxMetrics.failed();
            throw new PaymentException("Failed to create payment result outbox event");
        }
    }
}