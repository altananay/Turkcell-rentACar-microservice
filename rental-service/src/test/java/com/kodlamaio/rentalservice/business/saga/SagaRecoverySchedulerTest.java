package com.kodlamaio.rentalservice.business.saga;

import com.kodlamaio.rentalservice.entities.RentalCreationSaga;
import com.kodlamaio.rentalservice.entities.enums.SagaStatus;
import com.kodlamaio.rentalservice.repository.RentalCreationSagaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaRecoverySchedulerTest {

    @Mock private RentalCreationSagaRepository sagaRepository;
    @Mock private RentalCreationSagaOrchestrator orchestrator;

    @InjectMocks
    private SagaRecoveryScheduler scheduler;

    private RentalCreationSaga newSaga(SagaStatus status) {
        var saga = new RentalCreationSaga();
        saga.setId(UUID.randomUUID());
        saga.setStatus(status);
        return saga;
    }

    @Test
    void recoverStuckSagas_queriesRecoverableStatusesAndDrivesEachOne() {
        var stuckStarted = newSaga(SagaStatus.STARTED);
        var stuckCharged = newSaga(SagaStatus.PAYMENT_CHARGED);
        when(sagaRepository.findByStatusInAndUpdatedAtBefore(any(), any())).thenReturn(List.of(stuckStarted, stuckCharged));

        scheduler.recoverStuckSagas();

        var statusCaptor = ArgumentCaptor.forClass(List.class);
        verify(sagaRepository).findByStatusInAndUpdatedAtBefore(statusCaptor.capture(), any());
        assertThat(statusCaptor.getValue()).containsExactlyInAnyOrder(
                SagaStatus.STARTED, SagaStatus.PAYMENT_CHARGED, SagaStatus.COMPENSATING);

        verify(orchestrator).drive(stuckStarted);
        verify(orchestrator).drive(stuckCharged);
    }

    @Test
    void recoverStuckSagas_whenOneSagaDriveThrows_stillDrivesTheRest() {
        var failing = newSaga(SagaStatus.STARTED);
        var healthy = newSaga(SagaStatus.PAYMENT_CHARGED);
        when(sagaRepository.findByStatusInAndUpdatedAtBefore(any(), any())).thenReturn(List.of(failing, healthy));
        doThrow(new RuntimeException("still stuck")).when(orchestrator).drive(failing);

        scheduler.recoverStuckSagas();

        verify(orchestrator).drive(failing);
        verify(orchestrator).drive(healthy);
    }

    @Test
    void recoverStuckSagas_whenNoStaleSagasFound_neverCallsDrive() {
        when(sagaRepository.findByStatusInAndUpdatedAtBefore(any(), any())).thenReturn(List.of());

        scheduler.recoverStuckSagas();

        verifyNoInteractions(orchestrator);
    }
}
