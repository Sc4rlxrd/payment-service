package com.scarlxrd.payment_service.impl;

import com.scarlxrd.payment_service.entity.PaymentStatus;

public interface PaymentProcessor {
    PaymentStatus process();
}