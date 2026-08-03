package com.kodlamaio.rentalservice.business.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kodlamaio.commonpackage.events.Event;
import com.kodlamaio.commonpackage.events.rental.RentalDeletedEvent;
import com.kodlamaio.commonpackage.events.rentalPayment.RentalPaymentCreatedEvent;
import com.kodlamaio.rentalservice.entities.OutboxMessage;
import com.kodlamaio.rentalservice.repository.OutboxMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OutboxRecorderTest {

    @Mock private OutboxMessageRepository repository;

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @Test
    void record_storesTopicAndSerializedPayloadAsUnpublished() {
        var recorder = new OutboxRecorder(repository, objectMapper);
        var carId = UUID.randomUUID();

        recorder.record(new RentalDeletedEvent(carId), "rental-deleted");

        var captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTopic()).isEqualTo("rental-deleted");
        assertThat(captor.getValue().isPublished()).isFalse();
        assertThat(captor.getValue().getPublishedAt()).isNull();
        assertThat(captor.getValue().getPayload()).contains(carId.toString());
    }

    @Test
    void record_storesPayloadThatDeserializesBackToAnEqualEvent() throws Exception {
        var recorder = new OutboxRecorder(repository, objectMapper);
        var event = new RentalDeletedEvent(UUID.randomUUID());

        recorder.record(event, "rental-deleted");

        var captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(repository).save(captor.capture());
        var restored = objectMapper.readValue(captor.getValue().getPayload(), RentalDeletedEvent.class);
        assertThat(restored.getCarId()).isEqualTo(event.getCarId());
    }

    @Test
    void record_serializesRentalPaymentCreatedEventRentedAtAndDeserializesItBackToTheSameLocalDateTime() throws Exception {
        var recorder = new OutboxRecorder(repository, objectMapper);
        var event = new RentalPaymentCreatedEvent();
        event.setRentedAt(LocalDateTime.of(2026, 8, 3, 14, 30, 15));
        event.setRentalId(UUID.randomUUID());

        // Guards the JavaTimeModule: without it Jackson refuses LocalDateTime outright and every
        // rental-payment-created row would fail to serialize, silently ending invoice creation.
        recorder.record(event, "rental-payment-created");

        var captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(repository).save(captor.capture());
        var restored = objectMapper.readValue(captor.getValue().getPayload(), RentalPaymentCreatedEvent.class);
        assertThat(restored.getRentedAt()).isEqualTo(event.getRentedAt());
        assertThat(restored.getRentalId()).isEqualTo(event.getRentalId());
    }

    @Test
    void record_whenSerializationFails_throwsSoTheCallerTransactionRollsBack() {
        var recorder = new OutboxRecorder(repository, objectMapper);

        assertThatThrownBy(() -> recorder.record(new UnserializableEvent(), "rental-deleted"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("rental-deleted");

        verifyNoInteractions(repository);
    }

    // No getters, so Jackson has no properties to write and refuses to serialize it.
    private static class UnserializableEvent implements Event {
        private final String ignored = "ignored";
    }
}
