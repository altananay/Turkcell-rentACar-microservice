package com.kodlamaio.maintenanceservice.repository;

import com.kodlamaio.maintenanceservice.entities.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, UUID> {
    List<OutboxMessage> findTop100ByPublishedFalseOrderByCreatedAtAsc();
}
