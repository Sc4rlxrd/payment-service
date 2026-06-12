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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService - Processamento de pagamentos")
class PaymentServiceTest {

    @Mock
    private PaymentRepository repository;

    @Mock
    private PaymentProcessor processor;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PaymentMetrics paymentMetrics;

    @Mock
    private OutboxMetrics outboxMetrics;

    @Mock
    private RabbitEventMetrics rabbitMetrics;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequestDTO request;

    @BeforeEach
    void setUp() {
        request = new PaymentRequestDTO();
        request.setOrderId(UUID.randomUUID());
        request.setAmount(new BigDecimal("150.00"));
    }

    private void mockNoExistingPayment() {
        when(repository.findByOrderId(request.getOrderId()))
                .thenReturn(Optional.empty());
    }

    private void mockRepositorySave() {
        when(repository.saveAndFlush(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void mockObjectMapperSuccessfully() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any(PaymentResultEvent.class)))
                .thenReturn("{}");
    }

    @Nested
    @DisplayName("Quando o pagamento é bem-sucedido")
    class SuccessScenario {

        @Test
        @DisplayName("Deve retornar SUCCESS no resultado")
        void shouldReturnSuccessWhenProcessorReturnsSuccess() throws JsonProcessingException {

            // Given
            mockNoExistingPayment();
            mockRepositorySave();
            mockObjectMapperSuccessfully();

            when(processor.process()).thenReturn(PaymentStatus.SUCCESS);

            // When
            PaymentResultEvent result = paymentService.process(request);

            // Then
            assertThat(result.getEventId()).isNotNull();
            assertThat(result.getOrderId()).isEqualTo(request.getOrderId());
            assertThat(result.getStatus()).isEqualTo("SUCCESS");
            assertThat(result.getAmount()).isEqualByComparingTo(request.getAmount());

            verify(paymentMetrics).processed();
            verify(paymentMetrics).paymentSuccess();
            verify(outboxMetrics).created();
        }

        @Test
        @DisplayName("Deve salvar Payment com status SUCCESS")
        void shouldSavePaymentWithSuccessStatus() throws JsonProcessingException {

            // Given
            mockNoExistingPayment();
            mockRepositorySave();
            mockObjectMapperSuccessfully();

            when(processor.process()).thenReturn(PaymentStatus.SUCCESS);

            // When
            paymentService.process(request);

            // Then
            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(repository).saveAndFlush(captor.capture());

            Payment saved = captor.getValue();
            assertThat(saved.getOrderId()).isEqualTo(request.getOrderId());
            assertThat(saved.getAmount()).isEqualByComparingTo(request.getAmount());
            assertThat(saved.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        }

        @Test
        @DisplayName("Deve salvar evento PAYMENT_APPROVED na outbox")
        void shouldSavePaymentApprovedOutboxEvent() throws JsonProcessingException {

            // Given
            mockNoExistingPayment();
            mockRepositorySave();
            mockObjectMapperSuccessfully();

            when(processor.process()).thenReturn(PaymentStatus.SUCCESS);

            // When
            PaymentResultEvent result = paymentService.process(request);

            // Then
            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxRepository).save(captor.capture());

            OutboxEvent event = captor.getValue();

            assertThat(event.getEventId()).isEqualTo(result.getEventId());
            assertThat(event.getAggregateId()).isEqualTo(request.getOrderId());
            assertThat(event.getAggregateType()).isEqualTo("PAYMENT");
            assertThat(event.getEventType()).isEqualTo("PAYMENT_APPROVED");
            assertThat(event.getPayload()).isEqualTo("{}");
            assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(event.getRetryCount()).isZero();
            assertThat(event.getCreatedAt()).isNotNull();

            verify(outboxMetrics).created();
        }
    }

    @Nested
    @DisplayName("Quando o pagamento falha")
    class FailedScenario {

        @Test
        @DisplayName("Deve retornar FAILED no resultado")
        void shouldReturnFailedWhenProcessorReturnsFailed() throws JsonProcessingException {

            // Given
            mockNoExistingPayment();
            mockRepositorySave();
            mockObjectMapperSuccessfully();

            when(processor.process()).thenReturn(PaymentStatus.FAILED);

            // When
            PaymentResultEvent result = paymentService.process(request);

            // Then
            assertThat(result.getEventId()).isNotNull();
            assertThat(result.getOrderId()).isEqualTo(request.getOrderId());
            assertThat(result.getStatus()).isEqualTo("FAILED");
            assertThat(result.getAmount()).isEqualByComparingTo(request.getAmount());

            verify(paymentMetrics).processed();
            verify(paymentMetrics).paymentError("payment_failed");
            verify(outboxMetrics).created();
        }

        @Test
        @DisplayName("Deve salvar Payment com status FAILED")
        void shouldSavePaymentWithFailedStatus() throws JsonProcessingException {

            // Given
            mockNoExistingPayment();
            mockRepositorySave();
            mockObjectMapperSuccessfully();

            when(processor.process()).thenReturn(PaymentStatus.FAILED);

            // When
            paymentService.process(request);

            // Then
            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(repository).saveAndFlush(captor.capture());

            Payment saved = captor.getValue();
            assertThat(saved.getOrderId()).isEqualTo(request.getOrderId());
            assertThat(saved.getAmount()).isEqualByComparingTo(request.getAmount());
            assertThat(saved.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("Deve salvar evento PAYMENT_FAILED na outbox")
        void shouldSavePaymentFailedOutboxEvent() throws JsonProcessingException {

            // Given
            mockNoExistingPayment();
            mockRepositorySave();
            mockObjectMapperSuccessfully();

            when(processor.process()).thenReturn(PaymentStatus.FAILED);

            // When
            PaymentResultEvent result = paymentService.process(request);

            // Then
            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxRepository).save(captor.capture());

            OutboxEvent event = captor.getValue();

            assertThat(event.getEventId()).isEqualTo(result.getEventId());
            assertThat(event.getAggregateId()).isEqualTo(request.getOrderId());
            assertThat(event.getAggregateType()).isEqualTo("PAYMENT");
            assertThat(event.getEventType()).isEqualTo("PAYMENT_FAILED");
            assertThat(event.getPayload()).isEqualTo("{}");
            assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(event.getRetryCount()).isZero();
            assertThat(event.getCreatedAt()).isNotNull();

            verify(outboxMetrics).created();
        }
    }

    @Nested
    @DisplayName("Quando o serviço de pagamento está indisponível")
    class UnavailableScenario {

        @Test
        @DisplayName("Deve lançar PaymentException")
        void shouldThrowPaymentExceptionWhenUnavailable() {

            // Given
            mockNoExistingPayment();

            when(processor.process()).thenReturn(PaymentStatus.UNAVAILABLE);

            // When / Then
            assertThatThrownBy(() -> paymentService.process(request))
                    .isInstanceOf(PaymentException.class)
                    .hasMessage("Payment service unavailable");

            verify(paymentMetrics).processed();
            verify(paymentMetrics).paymentError("service_unavailable");
        }

        @Test
        @DisplayName("Não deve salvar Payment quando status for UNAVAILABLE")
        void shouldNotSavePaymentWhenUnavailable() {

            // Given
            mockNoExistingPayment();

            when(processor.process()).thenReturn(PaymentStatus.UNAVAILABLE);

            // When
            assertThatThrownBy(() -> paymentService.process(request))
                    .isInstanceOf(PaymentException.class);

            // Then
            verify(repository, never()).saveAndFlush(any(Payment.class));
            verify(outboxRepository, never()).save(any(OutboxEvent.class));

            verify(paymentMetrics).processed();
            verify(paymentMetrics).paymentError("service_unavailable");
        }
    }

    @Nested
    @DisplayName("Quando o pagamento já foi processado")
    class DuplicateScenario {

        @Test
        @DisplayName("Deve retornar pagamento existente quando orderId já foi processado")
        void shouldReturnExistingPaymentWhenOrderAlreadyProcessed() {

            // Given
            Payment existingPayment = Payment.builder()
                    .orderId(request.getOrderId())
                    .amount(request.getAmount())
                    .status(PaymentStatus.SUCCESS)
                    .build();

            when(repository.findByOrderId(request.getOrderId()))
                    .thenReturn(Optional.of(existingPayment));

            // When
            PaymentResultEvent result = paymentService.process(request);

            // Then
            assertThat(result.getEventId()).isNotNull();
            assertThat(result.getOrderId()).isEqualTo(request.getOrderId());
            assertThat(result.getStatus()).isEqualTo("SUCCESS");
            assertThat(result.getAmount()).isEqualByComparingTo(request.getAmount());

            verify(rabbitMetrics).duplicated("payment_process");
            verify(processor, never()).process();
            verify(repository, never()).saveAndFlush(any(Payment.class));
            verify(outboxRepository, never()).save(any(OutboxEvent.class));
        }

        @Test
        @DisplayName("Deve tratar duplicidade quando banco lançar DataIntegrityViolationException")
        void shouldHandleDuplicateWhenDatabaseThrowsDataIntegrityViolationException() {

            // Given
            Payment existingPayment = Payment.builder()
                    .orderId(request.getOrderId())
                    .amount(request.getAmount())
                    .status(PaymentStatus.SUCCESS)
                    .build();

            when(repository.findByOrderId(request.getOrderId()))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(existingPayment));

            when(processor.process()).thenReturn(PaymentStatus.SUCCESS);

            when(repository.saveAndFlush(any(Payment.class)))
                    .thenThrow(new DataIntegrityViolationException("Duplicate orderId"));

            // When
            PaymentResultEvent result = paymentService.process(request);

            // Then
            assertThat(result.getEventId()).isNotNull();
            assertThat(result.getOrderId()).isEqualTo(request.getOrderId());
            assertThat(result.getStatus()).isEqualTo("SUCCESS");
            assertThat(result.getAmount()).isEqualByComparingTo(request.getAmount());

            verify(rabbitMetrics).duplicated("payment_process");
            verify(outboxRepository, never()).save(any(OutboxEvent.class));
        }
    }

    @Nested
    @DisplayName("Quando falhar ao criar evento na outbox")
    class OutboxErrorScenario {

        @Test
        @DisplayName("Deve lançar PaymentException")
        void shouldThrowPaymentExceptionWhenOutboxSerializationFails() throws JsonProcessingException {

            // Given
            mockNoExistingPayment();
            mockRepositorySave();

            when(processor.process()).thenReturn(PaymentStatus.SUCCESS);

            when(objectMapper.writeValueAsString(any(PaymentResultEvent.class)))
                    .thenThrow(new JsonProcessingException("Erro ao serializar") {
                    });

            // When / Then
            assertThatThrownBy(() -> paymentService.process(request))
                    .isInstanceOf(PaymentException.class)
                    .hasMessage("Failed to create payment result outbox event");

            verify(outboxMetrics).failed();
            verify(outboxRepository, never()).save(any(OutboxEvent.class));
        }
    }
}