package com.kodlamaio.inventoryservice.entities;

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
@Table(name = "outbox_messages",
        indexes = @Index(name = "idx_outbox_unpublished", columnList = "published, created_at"))
public class OutboxMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Long version;

    private String topic;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private boolean published;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;
}
