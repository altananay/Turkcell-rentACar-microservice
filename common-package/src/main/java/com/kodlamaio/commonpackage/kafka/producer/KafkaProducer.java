package com.kodlamaio.commonpackage.kafka.producer;

import com.kodlamaio.commonpackage.events.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducer {

    private static final long ACKNOWLEDGEMENT_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public <T extends Event> void sendMessage(T event, String topic)
    {
        log.info(String.format("%s event => %s", topic, event.toString()));
        Message<T> message = MessageBuilder.withPayload(event).setHeader(KafkaHeaders.TOPIC, topic).build();

        kafkaTemplate.send(message);
    }

    // sendMessage above is fire-and-forget: it discards the send future, so a broker failure is never
    // observed. The outbox relay needs the opposite - it may only mark a row published once the broker
    // has actually acknowledged it, otherwise the outbox would record deliveries that never happened.
    public <T extends Event> void sendMessageAndWait(T event, String topic)
    {
        log.info(String.format("%s event => %s", topic, event.toString()));
        Message<T> message = MessageBuilder.withPayload(event).setHeader(KafkaHeaders.TOPIC, topic).build();

        try {
            kafkaTemplate.send(message).get(ACKNOWLEDGEMENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing to " + topic, exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("Kafka did not acknowledge publish to " + topic, exception);
        }
    }

}
