package com.example.notification_service.kafka;

import com.example.notification_service.notification.InvalidNotificationException;
import com.example.notification_service.notification.Notification;
import com.example.notification_service.notification.NotificationSender;
import com.example.notification_service.notification.NotificationType;
import com.example.notification_service.notification.Recipient;
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
                {"eventId":"e1","type":"WELCOME",
                 "recipient":{"id":"u1","firstName":"Jane","lastName":"Doe","email":"jane@example.com","phone":"+254700000000"},
                 "data":{"firstName":"Jane"},"extra":"ignored"}
                """);

        verify(sender).send(new Notification("e1", NotificationType.WELCOME,
                new Recipient("u1", "Jane", "Doe", "jane@example.com", "+254700000000"), Map.of("firstName", "Jane")));
    }

    @Test
    void missingData_defaultsToEmpty() {
        receive("""
                {"eventId":"e1","type":"WELCOME","recipient":{"id":"u1"}}
                """);

        verify(sender).send(new Notification("e1", NotificationType.WELCOME,
                new Recipient("u1", null, null, null, null), Map.of()));
    }

    @Test
    void missingEventId_isInvalid() {
        assertInvalid("""
                {"type":"WELCOME","recipient":{"id":"u1"}}
                """, "eventId");
    }

    @Test
    void missingType_isInvalid() {
        assertInvalid("""
                {"eventId":"e1","recipient":{"id":"u1"}}
                """, "type");
    }

    @Test
    void missingRecipientId_isInvalid() {
        assertInvalid("""
                {"eventId":"e1","type":"WELCOME","recipient":{"email":"jane@example.com"}}
                """, "recipient.id");
    }

    @Test
    void unknownType_isRejected() {
        assertThatThrownBy(() -> receive("""
                {"eventId":"e1","type":"NOPE","recipient":{"id":"u1"}}
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
                .isInstanceOf(InvalidNotificationException.class)
                .hasMessageContaining(field);
        verifyNoInteractions(sender);
    }
}
