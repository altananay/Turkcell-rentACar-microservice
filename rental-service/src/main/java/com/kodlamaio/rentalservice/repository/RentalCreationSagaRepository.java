package com.kodlamaio.rentalservice.repository;

import com.kodlamaio.rentalservice.entities.RentalCreationSaga;
import com.kodlamaio.rentalservice.entities.enums.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface RentalCreationSagaRepository extends JpaRepository<RentalCreationSaga, UUID> {
    List<RentalCreationSaga> findByStatusInAndUpdatedAtBefore(List<SagaStatus> statuses, LocalDateTime threshold);
}
