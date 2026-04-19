package com.scarlxrd.payment_service.config.rabbitmq;

import com.scarlxrd.payment_service.dto.PaymentResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishSuccess(PaymentResultEvent event) {
        rabbitTemplate.convertAndSend(
                "book.events",
                "payment.result.success",
                event
        );
    }

    public void publishFailed(PaymentResultEvent event) {
        rabbitTemplate.convertAndSend(
                "book.events",
                "payment.result.failed",
                event
        );
    }
}
