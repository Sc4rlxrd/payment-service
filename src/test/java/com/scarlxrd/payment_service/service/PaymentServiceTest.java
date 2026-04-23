package com.scarlxrd.payment_service.service;

import com.scarlxrd.payment_service.dto.PaymentRequestDTO;
import com.scarlxrd.payment_service.dto.PaymentResultEvent;
import com.scarlxrd.payment_service.entity.Payment;
import com.scarlxrd.payment_service.entity.PaymentStatus;
import com.scarlxrd.payment_service.exception.PaymentException;
import com.scarlxrd.payment_service.impl.PaymentProcessor;
import com.scarlxrd.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@DisplayName("PaymentService - Processamento de pagamentos")
class PaymentServiceTest {

    @Mock
    private PaymentRepository repository;

    @Mock
    private PaymentProcessor processor;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequestDTO request;

    @BeforeEach
    void setUp() {
        request = new PaymentRequestDTO();
        request.setOrderId(UUID.randomUUID());
        request.setAmount(new BigDecimal("150.00"));
    }

    @Nested
    @DisplayName("Quando o pagamento é bem-sucedido")
    class SuccessScenario {

        @Test
        @DisplayName("Deve retornar SUCCESS no resultado")
        void shouldReturnSuccessWhenProcessorReturnsSuccess() {

            // Given
            when(processor.process()).thenReturn(PaymentStatus.SUCCESS);

            // When
            PaymentResultEvent result = paymentService.process(request);

            // Then
            assertThat(result.getOrderId()).isEqualTo(request.getOrderId());
            assertThat(result.getStatus()).isEqualTo("SUCCESS");
            assertThat(result.getAmount()).isEqualByComparingTo(request.getAmount());
        }

        @Test
        @DisplayName("Deve salvar Payment com status SUCCESS")
        void shouldSavePaymentWithSuccessStatus() {

            // Given
            when(processor.process()).thenReturn(PaymentStatus.SUCCESS);

            // When
            paymentService.process(request);

            // Then
            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(repository).save(captor.capture());

            Payment saved = captor.getValue();
            assertThat(saved.getOrderId()).isEqualTo(request.getOrderId());
            assertThat(saved.getAmount()).isEqualByComparingTo(request.getAmount());
            assertThat(saved.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        }
    }

    @Nested
    @DisplayName("Quando o pagamento falha")
    class FailedScenario {

        @Test
        @DisplayName("Deve retornar FAILED no resultado")
        void shouldReturnFailedWhenProcessorReturnsFailed() {

            // Given
            when(processor.process()).thenReturn(PaymentStatus.FAILED);

            // When
            PaymentResultEvent result = paymentService.process(request);

            // Then
            assertThat(result.getOrderId()).isEqualTo(request.getOrderId());
            assertThat(result.getStatus()).isEqualTo("FAILED");
            assertThat(result.getAmount()).isEqualByComparingTo(request.getAmount());
        }

        @Test
        @DisplayName("Deve salvar Payment com status FAILED")
        void shouldSavePaymentWithFailedStatus() {

            // Given
            when(processor.process()).thenReturn(PaymentStatus.FAILED);

            // When
            paymentService.process(request);

            // Then
            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(repository).save(captor.capture());

            Payment saved = captor.getValue();
            assertThat(saved.getOrderId()).isEqualTo(request.getOrderId());
            assertThat(saved.getAmount()).isEqualByComparingTo(request.getAmount());
            assertThat(saved.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("Quando o serviço de pagamento está indisponível")
    class UnavailableScenario {

        @Test
        @DisplayName("Deve lançar PaymentException")
        void shouldThrowPaymentExceptionWhenUnavailable() {

            // Given
            when(processor.process()).thenReturn(PaymentStatus.UNAVAILABLE);

            // When / Then
            assertThatThrownBy(() -> paymentService.process(request))
                    .isInstanceOf(PaymentException.class)
                    .hasMessage("Serviço de pagamento indisponível");
        }

        @Test
        @DisplayName("Deve salvar Payment antes de lançar exceção")
        void shouldSavePaymentBeforeThrowingException() {

            // Given
            when(processor.process()).thenReturn(PaymentStatus.UNAVAILABLE);

            // When
            assertThatThrownBy(() -> paymentService.process(request))
                    .isInstanceOf(PaymentException.class);

            // Then
            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(repository).save(captor.capture());

            assertThat(captor.getValue().getStatus())
                    .isEqualTo(PaymentStatus.UNAVAILABLE);
        }
    }
}