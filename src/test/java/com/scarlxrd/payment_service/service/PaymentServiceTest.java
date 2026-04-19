package com.scarlxrd.payment_service.service;

import com.scarlxrd.payment_service.dto.OrderCreatedEvent;
import com.scarlxrd.payment_service.dto.PaymentResultEvent;
import com.scarlxrd.payment_service.entity.Payment;
import com.scarlxrd.payment_service.entity.PaymentStatus;
import com.scarlxrd.payment_service.exception.PaymentException;
import com.scarlxrd.payment_service.impl.PaymentProcessor;
import com.scarlxrd.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository repository;

    @Mock
    private PaymentProcessor processor;

    @InjectMocks
    private PaymentService paymentService;

    private OrderCreatedEvent event;

    @BeforeEach
    void setUp() {
        event = new OrderCreatedEvent(
                UUID.randomUUID(),
                new BigDecimal("150.00"),
                "cliente@email.com"
        );
    }



    @Test
    @DisplayName("Deve retornar SUCCESS quando processor retorna SUCCESS")
    void shouldReturnSuccessWhenProcessorReturnsSuccess() {
        when(processor.process()).thenReturn(PaymentStatus.SUCCESS);

        PaymentResultEvent result = paymentService.process(event);

        assertThat(result.getOrderId()).isEqualTo(event.getOrderId());
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getAmount()).isEqualByComparingTo(event.getAmount());
    }

    @Test
    @DisplayName("Deve salvar Payment com status SUCCESS e dados corretos")
    void shouldSavePaymentWithSuccessStatus() {
        when(processor.process()).thenReturn(PaymentStatus.SUCCESS);

        paymentService.process(event);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(repository).save(captor.capture());

        Payment saved = captor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(event.getOrderId());
        assertThat(saved.getAmount()).isEqualByComparingTo(event.getAmount());
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }



    @Test
    @DisplayName("Deve retornar FAILED quando processor retorna FAILED")
    void shouldReturnFailedWhenProcessorReturnsFailed() {
        when(processor.process()).thenReturn(PaymentStatus.FAILED);

        PaymentResultEvent result = paymentService.process(event);

        assertThat(result.getOrderId()).isEqualTo(event.getOrderId());
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getAmount()).isEqualByComparingTo(event.getAmount());
    }

    @Test
    @DisplayName("Deve salvar Payment com status FAILED e dados corretos")
    void shouldSavePaymentWithFailedStatus() {
        when(processor.process()).thenReturn(PaymentStatus.FAILED);

        paymentService.process(event);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(repository).save(captor.capture());

        Payment saved = captor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(event.getOrderId());
        assertThat(saved.getAmount()).isEqualByComparingTo(event.getAmount());
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }



    @Test
    @DisplayName("Deve lançar PaymentException quando processor retorna UNAVAILABLE")
    void shouldThrowPaymentExceptionWhenProcessorReturnsUnavailable() {
        when(processor.process()).thenReturn(PaymentStatus.UNAVAILABLE);

        assertThatThrownBy(() -> paymentService.process(event))
                .isInstanceOf(PaymentException.class)
                .hasMessage("Serviço de pagamento indisponível");
    }

    @Test
    @DisplayName("Deve salvar Payment com status UNAVAILABLE antes de lançar exceção")
    void shouldSavePaymentBeforeThrowingException() {
        when(processor.process()).thenReturn(PaymentStatus.UNAVAILABLE);

        assertThatThrownBy(() -> paymentService.process(event))
                .isInstanceOf(PaymentException.class);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.UNAVAILABLE);
    }
}