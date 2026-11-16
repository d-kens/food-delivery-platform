package com.example.notification_service.kafka;

import com.example.notification_service.event.UserEventHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class EventsListenerTest {

    private static final String VALUE = """
            {"eventId":"7f3c2a9e-5b1d-4e8a-9c0f-2d6b8e1a4f55","eventType":"UserRegistered",
             "occurredAt":"2026-11-16T10:15:30Z","userId":"u1"}
            """;

    private UserEventHandler userEventHandler;
    private EventsListener listener;

    @BeforeEach
    void setUp() {
        userEventHandler = mock(UserEventHandler.class);
        listener = new EventsListener(JsonMapper.builder().build(), userEventHandler, new KafkaTopics("user-events"));
    }

    @Test
    void userEvents_arePassedToUserHandlerWithEventType() {
        listener.onMessage(new ConsumerRecord<>("user-events", 0, 0L, "u1", VALUE));

        var captor = ArgumentCaptor.forClass(JsonNode.class);
        verify(userEventHandler).handle(eq("UserRegistered"), captor.capture());
        assertThat(captor.getValue().get("userId").asString()).isEqualTo("u1");
    }

    @Test
    void missingEventType_isRejected() {
        var record = new ConsumerRecord<>("user-events", 0, 0L, "u1", "{\"eventId\":\"e1\",\"userId\":\"u1\"}");

        assertThatThrownBy(() -> listener.onMessage(record))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eventType");
        verifyNoInteractions(userEventHandler);
    }

    @Test
    void missingEventId_isRejected() {
        var record = new ConsumerRecord<>("user-events", 0, 0L, "u1", "{\"eventType\":\"UserRegistered\",\"userId\":\"u1\"}");

        assertThatThrownBy(() -> listener.onMessage(record))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eventId");
        verifyNoInteractions(userEventHandler);
    }

    @Test
    void malformedJson_throwsJacksonException() {
        var record = new ConsumerRecord<>("user-events", 0, 0L, "u1", "hello");

        assertThatThrownBy(() -> listener.onMessage(record)).isInstanceOf(JacksonException.class);
        verifyNoInteractions(userEventHandler);
    }

    @Test
    void unknownTopic_isNotHandled() {
        listener.onMessage(new ConsumerRecord<>("other-events", 0, 0L, "o1", VALUE));

        verifyNoInteractions(userEventHandler);
    }
}
