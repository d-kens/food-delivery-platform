package com.example.notification_service.kafka;

import co.novu.models.errors.APIException;
import com.example.notification_service.notification.InvalidNotificationException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.util.backoff.BackOffExecution;
import tools.jackson.core.exc.StreamReadException;

import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaErrorHandlerConfigTest {

    private static final String DLT = "notifications.notification-service.DLT";

    private KafkaOperations<Object, Object> template;
    private DefaultErrorHandler handler;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        template = mock(KafkaOperations.class);
        when(template.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(null));
        handler = new KafkaErrorHandlerConfig().kafkaErrorHandler(template, "notification-service");
    }

    private boolean failOnce(Throwable cause) {
        var record = new ConsumerRecord<Object, Object>("notifications", 0, 0L, "u1", "{}");
        return handler.handleOne(new ListenerExecutionFailedException("listener failed", cause),
                record, mock(Consumer.class), mock(MessageListenerContainer.class));
    }

    @SuppressWarnings("unchecked")
    private static APIException novu(int code) {
        return new APIException("novu error", code, "{}".getBytes(), mock(HttpResponse.class), null);
    }

    @Test
    void invalidNotification_isDeadLetteredWithoutRetry() {
        assertThat(failOnce(new InvalidNotificationException("Notification is missing type"))).isTrue();
        verify(template).send(argThat((ProducerRecord<Object, Object> r) -> r.topic().equals(DLT)));
    }

    @Test
    void malformedJson_isDeadLetteredWithoutRetry() {
        assertThat(failOnce(new StreamReadException(null, "bad json"))).isTrue();
        verify(template).send(argThat((ProducerRecord<Object, Object> r) -> r.topic().equals(DLT)));
    }

    @Test
    void novuErrors_areRetried() {
        assertThat(failOnce(novu(400))).isFalse();
        verify(template, never()).send(any(ProducerRecord.class));
    }

    @Test
    void deadLetterTopic_isTopicGroupDlt() {
        assertThat(KafkaErrorHandlerConfig.deadLetterTopic("notifications", "notification-service")).isEqualTo(DLT);
    }

    @Test
    void retryBackOff_doublesWithJitterThenStops() {
        BackOffExecution execution = KafkaErrorHandlerConfig.retryBackOff().start();

        assertThat(execution.nextBackOff()).isBetween(1_000L, 1_500L);
        assertThat(execution.nextBackOff()).isBetween(1_000L, 3_000L);
        assertThat(execution.nextBackOff()).isBetween(2_000L, 6_000L);
        assertThat(execution.nextBackOff()).isBetween(4_000L, 12_000L);
        assertThat(execution.nextBackOff()).isEqualTo(BackOffExecution.STOP);
    }
}
