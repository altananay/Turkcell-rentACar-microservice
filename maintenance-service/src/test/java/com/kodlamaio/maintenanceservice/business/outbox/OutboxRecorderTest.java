package com.kodlamaio.maintenanceservice.business.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kodlamaio.commonpackage.events.Event;
import com.kodlamaio.commonpackage.events.maintenance.MaintenanceDeletedEvent;
import com.kodlamaio.maintenanceservice.entities.OutboxMessage;
import com.kodlamaio.maintenanceservice.repository.OutboxMessageRepository;
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

        recorder.record(new MaintenanceDeletedEvent(carId), "maintenance-deleted");

        var captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTopic()).isEqualTo("maintenance-deleted");
        assertThat(captor.getValue().isPublished()).isFalse();
        assertThat(captor.getValue().getPublishedAt()).isNull();
        assertThat(captor.getValue().getPayload()).contains(carId.toString());
    }

    @Test
    void record_storesPayloadThatDeserializesBackToAnEqualEvent() throws Exception {
        var recorder = new OutboxRecorder(repository, objectMapper);
        var event = new MaintenanceDeletedEvent(UUID.randomUUID());

        recorder.record(event, "maintenance-deleted");

        var captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(repository).save(captor.capture());
        var restored = objectMapper.readValue(captor.getValue().getPayload(), MaintenanceDeletedEvent.class);
        assertThat(restored.getCarId()).isEqualTo(event.getCarId());
    }

    @Test
    void record_whenSerializationFails_throwsSoTheCallerTransactionRollsBack() {
        var recorder = new OutboxRecorder(repository, objectMapper);

        assertThatThrownBy(() -> recorder.record(new UnserializableEvent(), "maintenance-deleted"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("maintenance-deleted");

        verifyNoInteractions(repository);
    }

    // No getters, so Jackson has no properties to write and refuses to serialize it.
    private static class UnserializableEvent implements Event {
        private final String ignored = "ignored";
    }
}
