package com.example.notification_service.kafka;

import com.example.notification_service.notification.InvalidNotificationException;
import com.example.notification_service.notification.NotificationSender;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
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
    void missingType_isInvalid() {
        assertInvalid("""
                {"eventId":"e1","recipient":{"id":"u1"}}
                """, "type");
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
