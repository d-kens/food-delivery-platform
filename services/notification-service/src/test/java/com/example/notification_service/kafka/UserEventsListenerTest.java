package com.example.notification_service.kafka;

import com.example.notification_service.user.UserEvent;
import com.example.notification_service.user.UserEventHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class UserEventsListenerTest {

    private UserEventHandler handler;
    private UserEventsListener listener;

    @BeforeEach
    void setUp() {
        handler = mock(UserEventHandler.class);
        listener = new UserEventsListener(JsonMapper.builder().build(), handler);
    }

    private void receive(String value) {
        listener.onMessage(new ConsumerRecord<>("user-events", 0, 0L, "u1", value));
    }

    @Test
    void validEvent_isHandled() {
        receive("""
                {"eventId":"e1","eventType":"UserCreated","occurredAt":"2026-11-18T09:00:00Z","userId":"u1",
                 "firstName":"Jane","lastName":"Doe","email":"jane@example.com","phone":"+254700000000"}
                """);

        verify(handler).handle(new UserEvent("e1", "UserCreated", "u1",
                "Jane", "Doe", "jane@example.com", "+254700000000"));
    }

    @Test
    void missingEventId_isInvalid() {
        assertInvalid("""
                {"eventType":"UserCreated","userId":"u1"}
                """, "eventId");
    }

    @Test
    void missingEventType_isInvalid() {
        assertInvalid("""
                {"eventId":"e1","userId":"u1"}
                """, "eventType");
    }

    @Test
    void missingUserId_isInvalid() {
        assertInvalid("""
                {"eventId":"e1","eventType":"UserCreated"}
                """, "userId");
    }

    @Test
    void malformedJson_isRejected() {
        assertThatThrownBy(() -> receive("hello")).isInstanceOf(JacksonException.class);
        verifyNoInteractions(handler);
    }

    private void assertInvalid(String json, String field) {
        Throwable thrown = catchThrowable(() -> receive(json));

        assertThat(thrown).isInstanceOf(JacksonException.class)
                .rootCause()
                .isInstanceOf(InvalidMessageException.class)
                .hasMessageContaining(field);
        verifyNoInteractions(handler);
    }
}
