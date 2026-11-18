package com.example.notification_service.kafka;

import com.example.notification_service.notification.Notification;
import com.example.notification_service.notification.NotificationSender;
import com.example.notification_service.notification.NotificationType;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class NotificationsListenerTest {

    private NotificationSender sender;
    private NotificationsListener listener;

    @BeforeEach
    void setUp() {
        sender = mock(NotificationSender.class);
        listener = new NotificationsListener(JsonMapper.builder().build(), sender);
    }

    private void receive(String value) {
        listener.onMessage(new ConsumerRecord<>("notifications", 0, 0L, "u1", value));
    }

    @Test
    void validNotification_isSent() {
        receive("""
                {"eventId":"e1","type":"UPDATED_PASSWORD","recipientId":"u1",
                 "payload":{"firstName":"Jane"},"extra":"ignored"}
                """);

        verify(sender).send(new Notification("e1", NotificationType.UPDATED_PASSWORD, "u1",
                Map.of("firstName", "Jane")));
    }

    @Test
    void missingPayload_defaultsToEmpty() {
        receive("""
                {"eventId":"e1","type":"UPDATED_PASSWORD","recipientId":"u1"}
                """);

        verify(sender).send(new Notification("e1", NotificationType.UPDATED_PASSWORD, "u1", Map.of()));
    }

    @Test
    void missingEventId_isInvalid() {
        assertInvalid("""
                {"type":"UPDATED_PASSWORD","recipientId":"u1"}
                """, "eventId");
    }

    @Test
    void missingType_isInvalid() {
        assertInvalid("""
                {"eventId":"e1","recipientId":"u1"}
                """, "type");
    }

    @Test
    void missingRecipientId_isInvalid() {
        assertInvalid("""
                {"eventId":"e1","type":"UPDATED_PASSWORD"}
                """, "recipientId");
    }

    @Test
    void unknownType_isRejected() {
        assertThatThrownBy(() -> receive("""
                {"eventId":"e1","type":"NOPE","recipientId":"u1"}
                """)).isInstanceOf(JacksonException.class);
        verifyNoInteractions(sender);
    }

    @Test
    void malformedJson_isRejected() {
        assertThatThrownBy(() -> receive("hello")).isInstanceOf(JacksonException.class);
        verifyNoInteractions(sender);
    }

    private void assertInvalid(String json, String field) {
        Throwable thrown = catchThrowable(() -> receive(json));

        assertThat(thrown).isInstanceOf(JacksonException.class)
                .rootCause()
                .isInstanceOf(InvalidMessageException.class)
                .hasMessageContaining(field);
        verifyNoInteractions(sender);
    }
}
