package com.scarlxrd.payment_service.config.rabbitmq;

import com.scarlxrd.payment_service.dto.OrderCreatedEvent;
import com.scarlxrd.payment_service.dto.PaymentResultEvent;
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
    private final PaymentPublisher paymentPublisher;

    @RabbitListener(queues = "payment.result.queue")
    public void handleOrderCreated(OrderCreatedEvent event) {

        log.info("Message received from order-service");
        try {
            PaymentResultEvent result = paymentService.process(event);

            if ("SUCCESS".equals(result.getStatus())) {
                paymentPublisher.publishSuccess(result);
            } else {
                paymentPublisher.publishFailed(result);
            }

        } catch (PaymentException ex) {
            throw ex;
        }
    }
}
