package com.kodlamaio.commonpackage.kafka.producer;

import com.kodlamaio.commonpackage.events.inventory.CarDeletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void sendMessageAndWait_whenBrokerAcknowledges_buildsTheSameMessageAndReturnsNormally() {
        var producer = new KafkaProducer(kafkaTemplate);
        var event = new CarDeletedEvent(UUID.randomUUID());
        when(kafkaTemplate.send(any(Message.class)))
                .thenReturn(CompletableFuture.completedFuture((SendResult<String, Object>) null));

        producer.sendMessageAndWait(event, "car-deleted");

        var captor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(captor.capture());
        assertThat(captor.getValue().getPayload()).isSameAs(event);
        assertThat(captor.getValue().getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo("car-deleted");
    }

    @Test
    void sendMessageAndWait_whenSendFutureCompletesExceptionally_throwsIllegalStateException() {
        var producer = new KafkaProducer(kafkaTemplate);
        var event = new CarDeletedEvent(UUID.randomUUID());
        var failed = new CompletableFuture<SendResult<String, Object>>();
        failed.completeExceptionally(new IllegalStateException("broker unreachable"));
        when(kafkaTemplate.send(any(Message.class))).thenReturn(failed);

        assertThatThrownBy(() -> producer.sendMessageAndWait(event, "car-deleted"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("did not acknowledge")
                .hasMessageContaining("car-deleted");
    }
}
