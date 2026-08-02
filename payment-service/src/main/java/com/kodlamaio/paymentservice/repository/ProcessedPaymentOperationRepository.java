package com.kodlamaio.paymentservice.repository;

import com.kodlamaio.paymentservice.entity.OperationType;
import com.kodlamaio.paymentservice.entity.ProcessedPaymentOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProcessedPaymentOperationRepository extends JpaRepository<ProcessedPaymentOperation, UUID> {
    Optional<ProcessedPaymentOperation> findByIdempotencyKeyAndOperationType(String idempotencyKey, OperationType operationType);
}
