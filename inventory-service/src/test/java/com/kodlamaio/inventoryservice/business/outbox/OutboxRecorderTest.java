package com.kodlamaio.inventoryservice.business.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kodlamaio.commonpackage.events.Event;
import com.kodlamaio.commonpackage.events.inventory.CarDeletedEvent;
import com.kodlamaio.inventoryservice.entities.OutboxMessage;
import com.kodlamaio.inventoryservice.repository.OutboxMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

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

        recorder.record(new CarDeletedEvent(carId), "car-deleted");

        var captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTopic()).isEqualTo("car-deleted");
        assertThat(captor.getValue().isPublished()).isFalse();
        assertThat(captor.getValue().getPublishedAt()).isNull();
        assertThat(captor.getValue().getPayload()).contains(carId.toString());
    }

    @Test
    void record_storesPayloadThatDeserializesBackToAnEqualEvent() throws Exception {
        var recorder = new OutboxRecorder(repository, objectMapper);
        var event = new CarDeletedEvent(UUID.randomUUID());

        recorder.record(event, "car-deleted");

        var captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(repository).save(captor.capture());
        var restored = objectMapper.readValue(captor.getValue().getPayload(), CarDeletedEvent.class);
        assertThat(restored.getCarId()).isEqualTo(event.getCarId());
    }

    @Test
    void record_whenSerializationFails_throwsSoTheCallerTransactionRollsBack() {
        var recorder = new OutboxRecorder(repository, objectMapper);

        assertThatThrownBy(() -> recorder.record(new UnserializableEvent(), "car-deleted"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("car-deleted");

        verifyNoInteractions(repository);
    }

    // No getters, so Jackson has no properties to write and refuses to serialize it.
    private static class UnserializableEvent implements Event {
        private final String ignored = "ignored";
    }
}
