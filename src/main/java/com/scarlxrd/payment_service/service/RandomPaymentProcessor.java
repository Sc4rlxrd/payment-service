package com.scarlxrd.payment_service.service;

import com.scarlxrd.payment_service.entity.PaymentStatus;
import com.scarlxrd.payment_service.impl.PaymentProcessor;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class RandomPaymentProcessor implements PaymentProcessor {

    private final Random random = new Random();

    @Override
    public PaymentStatus process() {
        int result = random.nextInt(100);

        if (result <= 70) return PaymentStatus.SUCCESS;
        if (result <= 90) return PaymentStatus.FAILED;

        return PaymentStatus.UNAVAILABLE;
    }
}