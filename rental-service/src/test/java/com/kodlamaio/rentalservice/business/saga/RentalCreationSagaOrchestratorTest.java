package com.kodlamaio.rentalservice.business.saga;

import com.kodlamaio.commonpackage.events.rental.RentalCreatedEvent;
import com.kodlamaio.commonpackage.events.rentalPayment.RentalPaymentCreatedEvent;
import com.kodlamaio.commonpackage.kafka.producer.KafkaProducer;
import com.kodlamaio.commonpackage.utils.dto.CarClientResponse;
import com.kodlamaio.commonpackage.utils.dto.ClientResponse;
import com.kodlamaio.commonpackage.utils.dto.CreateRentalPaymentRequest;
import com.kodlamaio.commonpackage.utils.dto.PaymentRequest;
import com.kodlamaio.commonpackage.utils.exceptions.BusinessException;
import com.kodlamaio.rentalservice.api.clients.CarClient;
import com.kodlamaio.rentalservice.api.clients.PaymentClient;
import com.kodlamaio.rentalservice.business.dto.requests.CreateRentalRequest;
import com.kodlamaio.rentalservice.entities.Rental;
import com.kodlamaio.rentalservice.entities.RentalCreationSaga;
import com.kodlamaio.rentalservice.entities.enums.SagaStatus;
import com.kodlamaio.rentalservice.repository.RentalCreationSagaRepository;
import com.kodlamaio.rentalservice.repository.RentalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalCreationSagaOrchestratorTest {

    @Mock private RentalCreationSagaRepository sagaRepository;
    @Mock private RentalRepository rentalRepository;
    @Mock private PaymentClient paymentClient;
    @Mock private CarClient carClient;
    @Mock private KafkaProducer producer;
    @Mock private TransactionTemplate transactionTemplate;

    @InjectMocks
    private RentalCreationSagaOrchestrator orchestrator;

    @BeforeEach
    void stubTransactionTemplateToRunLambda() {
        lenient().doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    private RentalCreationSaga newSaga(SagaStatus status) {
        var saga = new RentalCreationSaga();
        saga.setId(UUID.randomUUID());
        saga.setRentalId(UUID.randomUUID());
        saga.setCarId(UUID.randomUUID());
        saga.setDailyPrice(100.0);
        saga.setRentedForDays(3);
        saga.setPrice(300.0);
        saga.setCardNumber("1234567890123456");
        saga.setCardHolder("John Doe");
        saga.setCardExpirationYear(2025);
        saga.setCardExpirationMonth(6);
        saga.setCardCvv("123");
        saga.setStatus(status);
        saga.setCreatedAt(LocalDateTime.now());
        return saga;
    }

    @Test
    void createRental_happyPath_chargesPaymentSavesRentalAndPublishesBothEvents() {
        var carId = UUID.randomUUID();
        var paymentRequest = new PaymentRequest("1234567890123456", "John Doe", 2025, 6, "123");
        var request = new CreateRentalRequest(carId, 100.0, 3, paymentRequest);

        // @CreationTimestamp only fires on a real Hibernate persist; this mock stub
        // reproduces that side effect so buildRental()'s saga.getCreatedAt() call below
        // doesn't NPE against a saga that was never actually persisted.
        lenient().when(sagaRepository.save(any())).thenAnswer(inv -> {
            RentalCreationSaga saga = inv.getArgument(0);
            if (saga.getCreatedAt() == null) {
                saga.setCreatedAt(LocalDateTime.now());
            }
            return saga;
        });
        when(paymentClient.processRentalPayment(anyString(), any())).thenReturn(new ClientResponse(true, null));
        var carClientResponse = new CarClientResponse();
        carClientResponse.setModelName("Corolla");
        carClientResponse.setBrandName("Toyota");
        carClientResponse.setPlate("34ABC34");
        carClientResponse.setModelYear(2022);
        when(carClient.getCar(carId)).thenReturn(carClientResponse);
        when(rentalRepository.findById(any())).thenReturn(Optional.empty());
        when(rentalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = orchestrator.createRental(request);

        // sagaRepository.save(saga) is called 3x with the SAME mutable saga reference
        // (initial STARTED, then PAYMENT_CHARGED, then COMPLETED) — an ArgumentCaptor only
        // ever sees the object's final in-memory state, so only the end state is assertable.
        var sagaCaptor = ArgumentCaptor.forClass(RentalCreationSaga.class);
        verify(sagaRepository, atLeast(3)).save(sagaCaptor.capture());
        var saga = sagaCaptor.getValue();
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);

        var rentalCaptor = ArgumentCaptor.forClass(Rental.class);
        verify(rentalRepository).save(rentalCaptor.capture());
        var savedRental = rentalCaptor.getValue();
        assertThat(savedRental.getId()).isEqualTo(saga.getRentalId());
        assertThat(savedRental.getCarId()).isEqualTo(carId);
        assertThat(savedRental.getTotalPrice()).isEqualTo(300.0);
        assertThat(savedRental.getRentedAt()).isEqualTo(saga.getCreatedAt().toLocalDate());

        var createdEventCaptor = ArgumentCaptor.forClass(RentalCreatedEvent.class);
        verify(producer).sendMessage(createdEventCaptor.capture(), eq("rental-created"));
        assertThat(createdEventCaptor.getValue().getCarId()).isEqualTo(carId);
        var paymentEventCaptor = ArgumentCaptor.forClass(RentalPaymentCreatedEvent.class);
        verify(producer).sendMessage(paymentEventCaptor.capture(), eq("rental-payment-created"));
        assertThat(paymentEventCaptor.getValue().getModelName()).isEqualTo("Corolla");
        assertThat(paymentEventCaptor.getValue().getTotalPrice()).isEqualTo(300.0);

        assertThat(result).isSameAs(savedRental);
    }

    @Test
    void drive_whenPaymentDeclined_marksPaymentFailedAndRethrowsWithoutRefunding() {
        var saga = newSaga(SagaStatus.STARTED);
        when(paymentClient.processRentalPayment(eq(saga.getId().toString()), any()))
                .thenReturn(new ClientResponse(false, "payment declined"));

        assertThatThrownBy(() -> orchestrator.drive(saga))
                .isInstanceOf(BusinessException.class)
                .hasMessage("payment declined");

        assertThat(saga.getStatus()).isEqualTo(SagaStatus.PAYMENT_FAILED);
        verifyNoInteractions(carClient);
        verify(paymentClient, never()).refundRentalPayment(any(), any());
    }

    @Test
    void drive_whenPaymentClientThrows_marksPaymentFailedAndRethrowsOriginalException() {
        var saga = newSaga(SagaStatus.STARTED);
        var feignFailure = new BusinessException("PAYMENT DOWN");
        when(paymentClient.processRentalPayment(eq(saga.getId().toString()), any())).thenThrow(feignFailure);

        assertThatThrownBy(() -> orchestrator.drive(saga)).isSameAs(feignFailure);

        assertThat(saga.getStatus()).isEqualTo(SagaStatus.PAYMENT_FAILED);
        verify(paymentClient, never()).refundRentalPayment(any(), any());
    }

    @Test
    void drive_whenCarClientFailsAfterCharge_compensatesAndRethrowsOriginalException() {
        var saga = newSaga(SagaStatus.STARTED);
        when(paymentClient.processRentalPayment(eq(saga.getId().toString()), any())).thenReturn(new ClientResponse(true, null));
        when(rentalRepository.findById(saga.getRentalId())).thenReturn(Optional.empty());
        var inventoryFailure = new RuntimeException("inventory down");
        when(carClient.getCar(saga.getCarId())).thenThrow(inventoryFailure);
        when(paymentClient.refundRentalPayment(eq(saga.getId().toString()), any())).thenReturn(new ClientResponse(true, null));

        assertThatThrownBy(() -> orchestrator.drive(saga)).isSameAs(inventoryFailure);

        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATED);
        verify(paymentClient).refundRentalPayment(eq(saga.getId().toString()), any());
        verifyNoInteractions(producer);
    }

    @Test
    void drive_whenRentalSaveFailsInsideTransaction_compensatesAndRethrowsOriginalException() {
        var saga = newSaga(SagaStatus.STARTED);
        when(paymentClient.processRentalPayment(eq(saga.getId().toString()), any())).thenReturn(new ClientResponse(true, null));
        when(rentalRepository.findById(saga.getRentalId())).thenReturn(Optional.empty());
        when(carClient.getCar(saga.getCarId())).thenReturn(new CarClientResponse());
        var dbFailure = new RuntimeException("db down");
        when(rentalRepository.save(any())).thenThrow(dbFailure);
        when(paymentClient.refundRentalPayment(eq(saga.getId().toString()), any())).thenReturn(new ClientResponse(true, null));

        assertThatThrownBy(() -> orchestrator.drive(saga)).isSameAs(dbFailure);

        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATED);
        verify(paymentClient).refundRentalPayment(eq(saga.getId().toString()), any());
    }

    @Test
    void drive_whenCompensationRefundFails_marksCompensationFailedButRethrowsOriginalCause() {
        var saga = newSaga(SagaStatus.STARTED);
        when(paymentClient.processRentalPayment(eq(saga.getId().toString()), any())).thenReturn(new ClientResponse(true, null));
        when(rentalRepository.findById(saga.getRentalId())).thenReturn(Optional.empty());
        var inventoryFailure = new RuntimeException("inventory down");
        when(carClient.getCar(saga.getCarId())).thenThrow(inventoryFailure);
        when(paymentClient.refundRentalPayment(eq(saga.getId().toString()), any()))
                .thenThrow(new RuntimeException("refund gateway down"));

        assertThatThrownBy(() -> orchestrator.drive(saga)).isSameAs(inventoryFailure);

        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATION_FAILED);
    }

    @Test
    void drive_whenResumingFromPaymentCharged_neverChargesAgainAndCompletesRental() {
        var saga = newSaga(SagaStatus.PAYMENT_CHARGED);
        when(rentalRepository.findById(saga.getRentalId())).thenReturn(Optional.empty());
        when(carClient.getCar(saga.getCarId())).thenReturn(new CarClientResponse());
        when(rentalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = orchestrator.drive(saga);

        verify(paymentClient, never()).processRentalPayment(any(), any());
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(result).isNotNull();
    }

    @Test
    void drive_whenResumingFromCompensating_onlyRetriesRefundAndNeverTouchesRentalOrCar() {
        var saga = newSaga(SagaStatus.COMPENSATING);
        when(paymentClient.refundRentalPayment(eq(saga.getId().toString()), any())).thenReturn(new ClientResponse(true, null));

        var result = orchestrator.drive(saga);

        assertThat(result).isNull();
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATED);
        verifyNoInteractions(rentalRepository, carClient, producer);
    }

    @Test
    void drive_whenRentalAlreadyExists_returnsExistingRentalWithoutResavingOrPublishing() {
        var saga = newSaga(SagaStatus.PAYMENT_CHARGED);
        var existingRental = new Rental();
        existingRental.setId(saga.getRentalId());
        when(rentalRepository.findById(saga.getRentalId())).thenReturn(Optional.of(existingRental));

        var result = orchestrator.drive(saga);

        assertThat(result).isSameAs(existingRental);
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);
        verify(rentalRepository, never()).save(any());
        verifyNoInteractions(carClient, producer);
    }

    @Test
    void drive_whenSagaSaveRacesOptimistically_skipsWithoutRefundingOrRethrowing() {
        var saga = newSaga(SagaStatus.STARTED);
        when(paymentClient.processRentalPayment(eq(saga.getId().toString()), any())).thenReturn(new ClientResponse(true, null));
        doThrow(new OptimisticLockingFailureException("stale row"))
                .when(sagaRepository).save(argThat(s -> s != null && s.getStatus() == SagaStatus.PAYMENT_CHARGED));

        var result = orchestrator.drive(saga);

        assertThat(result).isNull();
        verify(paymentClient, never()).refundRentalPayment(any(), any());
    }

    @Test
    void drive_whenCharging_sendsPaymentRequestRebuiltExactlyFromSagaFields() {
        var saga = newSaga(SagaStatus.STARTED);
        when(paymentClient.processRentalPayment(eq(saga.getId().toString()), any())).thenReturn(new ClientResponse(true, null));
        when(rentalRepository.findById(saga.getRentalId())).thenReturn(Optional.empty());
        when(carClient.getCar(saga.getCarId())).thenReturn(new CarClientResponse());
        when(rentalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orchestrator.drive(saga);

        var captor = ArgumentCaptor.forClass(CreateRentalPaymentRequest.class);
        verify(paymentClient).processRentalPayment(eq(saga.getId().toString()), captor.capture());
        var sent = captor.getValue();
        assertThat(sent.getCardNumber()).isEqualTo(saga.getCardNumber());
        assertThat(sent.getCardHolder()).isEqualTo(saga.getCardHolder());
        assertThat(sent.getCardExpirationYear()).isEqualTo(saga.getCardExpirationYear());
        assertThat(sent.getCardExpirationMonth()).isEqualTo(saga.getCardExpirationMonth());
        assertThat(sent.getCardCvv()).isEqualTo(saga.getCardCvv());
        assertThat(sent.getPrice()).isEqualTo(saga.getPrice());
    }

    @Test
    void drive_derivesRentedAtFromSagaCreatedAtNotFromNow() {
        var saga = newSaga(SagaStatus.PAYMENT_CHARGED);
        saga.setCreatedAt(LocalDateTime.now().minusDays(5));
        when(rentalRepository.findById(saga.getRentalId())).thenReturn(Optional.empty());
        when(carClient.getCar(saga.getCarId())).thenReturn(new CarClientResponse());
        var rentalCaptor = ArgumentCaptor.forClass(Rental.class);
        when(rentalRepository.save(rentalCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        orchestrator.drive(saga);

        assertThat(rentalCaptor.getValue().getRentedAt()).isEqualTo(saga.getCreatedAt().toLocalDate());
    }

    @Test
    void drive_whenKafkaPublishFailsAfterCommit_doesNotCompensate() {
        var saga = newSaga(SagaStatus.PAYMENT_CHARGED);
        when(rentalRepository.findById(saga.getRentalId())).thenReturn(Optional.empty());
        when(carClient.getCar(saga.getCarId())).thenReturn(new CarClientResponse());
        when(rentalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("kafka down")).when(producer).sendMessage(any(), anyString());

        var result = orchestrator.drive(saga);

        assertThat(result).isNotNull();
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);
        verifyNoInteractions(paymentClient);
    }
}
