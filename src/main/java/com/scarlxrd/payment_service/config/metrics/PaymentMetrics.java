package com.scarlxrd.payment_service.config.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentMetrics {

    private final MeterRegistry meterRegistry;


    public void processed() {
        Counter.builder("payments_processed_total")
                .description("Total de pagamentos processados")
                .tag("service", "payment-service")
                .register(this.meterRegistry)
                .increment();
    }

    public void paymentSuccess() {

        Counter.builder("payments_success_total")
                .description("Total  de pagamentos processados com sucesso")
                .tag("service", "payment-service")
                .register(this.meterRegistry)
                .increment();
    }


    public void paymentError(String reason) {
        Counter.builder("payments_failed_total")
                .description("total de pagamentos processados com erro")
                .tag("service", "payment-service")
                .tag("reason", reason)
                .register(this.meterRegistry)
                .increment();
    }

}
