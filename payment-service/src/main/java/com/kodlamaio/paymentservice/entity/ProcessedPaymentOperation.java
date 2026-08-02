package com.kodlamaio.paymentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "processed_payment_operations", uniqueConstraints = @UniqueConstraint(columnNames = {"idempotencyKey", "operationType"}))
public class ProcessedPaymentOperation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    private OperationType operationType;

    private boolean success;
    private String message;

    @CreationTimestamp
    private LocalDateTime processedAt;
}
