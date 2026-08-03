package com.kodlamaio.rentalservice.business.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kodlamaio.commonpackage.events.Event;
import com.kodlamaio.commonpackage.events.rental.RentalCreatedEvent;
import com.kodlamaio.commonpackage.events.rental.RentalDeletedEvent;
import com.kodlamaio.commonpackage.events.rentalPayment.RentalPaymentCreatedEvent;
import com.kodlamaio.commonpackage.kafka.producer.KafkaProducer;
import com.kodlamaio.rentalservice.entities.OutboxMessage;
import com.kodlamaio.rentalservice.repository.OutboxMessageRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@AllArgsConstructor
public class OutboxRelay {
    // Topic to event type, instead of storing the class name on the row: a package rename then becomes
    // a compile error rather than a ClassNotFoundException on rows written months earlier.
    private static final Map<String, Class<? extends Event>> TOPIC_TYPES = Map.of(
            "rental-created", RentalCreatedEvent.class,
            "rental-deleted", RentalDeletedEvent.class,
            "rental-payment-created", RentalPaymentCreatedEvent.class);

    private final OutboxMessageRepository repository;
    private final ObjectMapper objectMapper;
    private final KafkaProducer producer;

    @Scheduled(fixedDelay = 5000)
    public void publishPending() {
        for (var message : repository.findTop100ByPublishedFalseOrderByCreatedAtAsc()) {
            var event = deserialize(message);
            if (Objects.isNull(event)) {
                continue;
            }
            try {
                producer.sendMessageAndWait(event, message.getTopic());
            } catch (Exception exception) {
                // Abandon the batch, not just this row. A publish failure means the broker is
                // unreachable, so the remaining rows would fail too - and each attempt blocks the
                // single shared scheduler thread, which would otherwise starve SagaRecoveryScheduler
                // for the whole outage, leaving charged-but-unsaved rentals unrecovered.
                log.warn("Publishing outbox row {} failed, abandoning this batch: {}",
                        message.getId(), exception.getMessage());
                return;
            }
            markPublished(message);
        }
    }

    private Event deserialize(OutboxMessage message) {
        var type = TOPIC_TYPES.get(message.getTopic());
        if (Objects.isNull(type)) {
            log.error("Outbox row {} has unknown topic {}, it will never be published",
                    message.getId(), message.getTopic());
            return null;
        }
        try {
            return objectMapper.readValue(message.getPayload(), type);
        } catch (Exception exception) {
            log.error("Outbox row {} has an unreadable payload, it will never be published: {}",
                    message.getId(), exception.getMessage());
            return null;
        }
    }

    private void markPublished(OutboxMessage message) {
        message.setPublished(true);
        message.setPublishedAt(LocalDateTime.now());
        try {
            repository.save(message);
        } catch (OptimisticLockingFailureException raced) {
            // Delivery is at-least-once: the claim happens after the send, so a concurrent relay has
            // already published this row too.
            log.warn("Outbox row {} was already marked published by another instance - it was published more than once",
                    message.getId());
        }
    }
}
