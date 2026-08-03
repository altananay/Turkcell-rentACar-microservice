package com.kodlamaio.rentalservice.business.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kodlamaio.commonpackage.events.rental.RentalDeletedEvent;
import com.kodlamaio.commonpackage.kafka.producer.KafkaProducer;
import com.kodlamaio.rentalservice.entities.OutboxMessage;
import com.kodlamaio.rentalservice.repository.OutboxMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock private OutboxMessageRepository repository;
    @Mock private KafkaProducer producer;

    @Spy private ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @InjectMocks
    private OutboxRelay relay;

    private OutboxMessage row(String topic, String payload) {
        var message = new OutboxMessage();
        message.setId(UUID.randomUUID());
        message.setTopic(topic);
        message.setPayload(payload);
        message.setPublished(false);
        return message;
    }

    private OutboxMessage rentalDeletedRow() {
        return row("rental-deleted", "{\"carId\":\"" + UUID.randomUUID() + "\"}");
    }

    @Test
    void publishPending_whenNoUnpublishedRows_neverTouchesProducer() {
        when(repository.findTop100ByPublishedFalseOrderByCreatedAtAsc()).thenReturn(List.of());

        relay.publishPending();

        verifyNoInteractions(producer);
        verify(repository, never()).save(any());
    }

    @Test
    void publishPending_whenRowIsAcknowledged_marksItPublishedWithTimestamp() {
        var message = rentalDeletedRow();
        when(repository.findTop100ByPublishedFalseOrderByCreatedAtAsc()).thenReturn(List.of(message));

        relay.publishPending();

        verify(producer).sendMessageAndWait(any(RentalDeletedEvent.class), eq("rental-deleted"));
        var captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isPublished()).isTrue();
        assertThat(captor.getValue().getPublishedAt()).isNotNull();
    }

    @Test
    void publishPending_publishesRowsOldestFirstInTheOrderTheRepositoryReturnsThem() {
        var first = rentalDeletedRow();
        var second = rentalDeletedRow();
        when(repository.findTop100ByPublishedFalseOrderByCreatedAtAsc()).thenReturn(List.of(first, second));

        relay.publishPending();

        var order = inOrder(repository, producer);
        order.verify(producer).sendMessageAndWait(any(RentalDeletedEvent.class), eq("rental-deleted"));
        order.verify(repository).save(first);
        order.verify(producer).sendMessageAndWait(any(RentalDeletedEvent.class), eq("rental-deleted"));
        order.verify(repository).save(second);
    }

    @Test
    void publishPending_usesTheBlockingSendSoAnUnacknowledgedMessageIsNotMarkedPublished() {
        var message = rentalDeletedRow();
        when(repository.findTop100ByPublishedFalseOrderByCreatedAtAsc()).thenReturn(List.of(message));

        relay.publishPending();

        // The fire-and-forget sendMessage would return before the broker acknowledged anything.
        verify(producer, never()).sendMessage(any(), anyString());
        verify(producer).sendMessageAndWait(any(RentalDeletedEvent.class), eq("rental-deleted"));
    }

    @Test
    void publishPending_whenPayloadCannotBeDeserialized_leavesRowUnpublishedAndProcessesTheNextRow() {
        var broken = row("rental-deleted", "not json");
        var healthy = rentalDeletedRow();
        when(repository.findTop100ByPublishedFalseOrderByCreatedAtAsc()).thenReturn(List.of(broken, healthy));

        relay.publishPending();

        verify(repository, never()).save(broken);
        verify(repository).save(healthy);
        verify(producer, times(1)).sendMessageAndWait(any(), anyString());
    }

    @Test
    void publishPending_whenTopicHasNoRegisteredEventType_leavesRowUnpublishedAndProcessesTheNextRow() {
        var unknown = row("brand-deleted", "{}");
        var healthy = rentalDeletedRow();
        when(repository.findTop100ByPublishedFalseOrderByCreatedAtAsc()).thenReturn(List.of(unknown, healthy));

        relay.publishPending();

        verify(repository, never()).save(unknown);
        verify(repository).save(healthy);
    }

    @Test
    void publishPending_whenPublishFails_leavesRowUnpublishedAndAbandonsTheRestOfTheBatch() {
        var failing = rentalDeletedRow();
        var never = rentalDeletedRow();
        when(repository.findTop100ByPublishedFalseOrderByCreatedAtAsc()).thenReturn(List.of(failing, never));
        doThrow(new IllegalStateException("Kafka did not acknowledge publish to car-deleted"))
                .when(producer).sendMessageAndWait(any(), anyString());

        relay.publishPending();

        // Abandoning the batch is what keeps a Kafka outage from monopolising the shared scheduler thread.
        verify(producer, times(1)).sendMessageAndWait(any(), anyString());
        verify(repository, never()).save(any());
    }

    @Test
    void publishPending_whenMarkingPublishedLosesAnOptimisticLock_continuesWithTheNextRow() {
        var raced = rentalDeletedRow();
        var healthy = rentalDeletedRow();
        when(repository.findTop100ByPublishedFalseOrderByCreatedAtAsc()).thenReturn(List.of(raced, healthy));
        doThrow(new OptimisticLockingFailureException("already claimed")).when(repository).save(raced);

        relay.publishPending();

        verify(producer, times(2)).sendMessageAndWait(any(), anyString());
        verify(repository).save(healthy);
    }
}
