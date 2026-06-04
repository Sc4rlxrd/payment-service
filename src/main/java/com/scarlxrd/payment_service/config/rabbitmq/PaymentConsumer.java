package com.scarlxrd.payment_service.config.rabbitmq;

import com.scarlxrd.payment_service.dto.PaymentRequestDTO;
import com.scarlxrd.payment_service.dto.PaymentResultEvent;
import com.scarlxrd.payment_service.entity.PaymentStatus;
import com.scarlxrd.payment_service.exception.PaymentException;
import com.scarlxrd.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentConsumer {

    private final PaymentService paymentService;

    @RabbitListener(
            queues = "payment.process.queue",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleOrderCreated(PaymentRequestDTO event) {

        log.info(
                "Message received | orderId={} amount={}",
                event.getOrderId(),
                event.getAmount()
        );

        try {
            PaymentResultEvent result = paymentService.process(event);

            log.info(
                    "Payment processed and outbox event created | orderId={} status={}",
                    result.getOrderId(),
                    result.getStatus()
            );

        } catch (PaymentException ex) {
            log.error("Error processing payment | orderId={}", event.getOrderId(), ex);
            throw ex;
        }
    }
}