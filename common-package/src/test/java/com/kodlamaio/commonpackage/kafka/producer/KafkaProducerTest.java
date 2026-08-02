package com.kodlamaio.commonpackage.kafka.producer;

import com.kodlamaio.commonpackage.events.inventory.CarDeletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void sendMessage_buildsMessageWithPayloadAndTopicHeader_andSendsViaTemplate() {
        var producer = new KafkaProducer(kafkaTemplate);
        var event = new CarDeletedEvent(UUID.randomUUID());

        producer.sendMessage(event, "car-deleted");

        var captor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(captor.capture());
        assertThat(captor.getValue().getPayload()).isSameAs(event);
        assertThat(captor.getValue().getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo("car-deleted");
    }
}
