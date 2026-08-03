package com.kodlamaio.inventoryservice.business.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kodlamaio.commonpackage.events.Event;
import com.kodlamaio.inventoryservice.entities.OutboxMessage;
import com.kodlamaio.inventoryservice.repository.OutboxMessageRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class OutboxRecorder {
    private final OutboxMessageRepository repository;
    private final ObjectMapper objectMapper;

    // Must be called from inside the caller's transaction so the row commits with the business entity.
    public void record(Event event, String topic) {
        var message = new OutboxMessage();
        message.setTopic(topic);
        message.setPayload(serialize(event, topic));
        message.setPublished(false);
        repository.save(message);
    }

    private String serialize(Event event, String topic) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            // Thrown, not swallowed: the caller's transaction must roll back rather than commit a
            // business row whose event could never be delivered.
            throw new IllegalStateException("Could not serialize event for topic " + topic, exception);
        }
    }
}
