package com.kodlamaio.rentalservice.business.saga;

import com.kodlamaio.rentalservice.entities.enums.SagaStatus;
import com.kodlamaio.rentalservice.repository.RentalCreationSagaRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class SagaRecoveryScheduler {
    private static final List<SagaStatus> RECOVERABLE = List.of(SagaStatus.STARTED, SagaStatus.PAYMENT_CHARGED, SagaStatus.COMPENSATING);

    private final RentalCreationSagaRepository sagaRepository;
    private final RentalCreationSagaOrchestrator orchestrator;

    @Scheduled(fixedDelay = 30000)
    public void recoverStuckSagas() {
        var cutoff = LocalDateTime.now().minusSeconds(60);
        for (var saga : sagaRepository.findByStatusInAndUpdatedAtBefore(RECOVERABLE, cutoff)) {
            try {
                orchestrator.drive(saga);
            } catch (Exception e) {
                log.warn("Recovery attempt failed for saga {}, will retry next cycle: {}", saga.getId(), e.getMessage());
            }
        }
    }
}
