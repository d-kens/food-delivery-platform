package com.example.notification_service.event;

import com.example.notification_service.novu.NotificationWorkflow;
import com.example.notification_service.novu.NovuClient;
import com.example.notification_service.novu.Subscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class UserEventHandlerTest {

    private static final String EVENT_ID = "7f3c2a9e-5b1d-4e8a-9c0f-2d6b8e1a4f55";
    private static final String USER_ID = "a1b2c3d4-0000-4000-8000-000000000001";
    private static final String OCCURRED_AT = "2026-11-16T10:15:30Z";
    private static final Subscriber JANE = new Subscriber(USER_ID, "Jane", "Doe", "jane@example.com", "+254700000000");

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private NovuClient novuClient;
    private UserEventHandler handler;

    @BeforeEach
    void setUp() {
        novuClient = mock(NovuClient.class);
        handler = new UserEventHandler(novuClient, jsonMapper);
    }

    private void handle(String eventType, String fields) {
        String json = """
                {"eventId":"%s","eventType":"%s","occurredAt":"%s","userId":"%s"%s}
                """.formatted(EVENT_ID, eventType, OCCURRED_AT, USER_ID, fields);
        handler.handle(eventType, jsonMapper.readTree(json));
    }

    private static final String PROFILE =
            ",\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"email\":\"jane@example.com\",\"phone\":\"+254700000000\"";

    @Test
    void userRegistered_upsertsSubscriberThenSendsWelcome() {
        handle("UserRegistered", PROFILE);

        InOrder order = inOrder(novuClient);
        order.verify(novuClient).upsertSubscriber(JANE);
        order.verify(novuClient).trigger(NotificationWorkflow.WELCOME, USER_ID,
                Map.of("eventId", EVENT_ID, "eventType", "UserRegistered", "occurredAt", OCCURRED_AT, "userId", USER_ID,
                        "firstName", "Jane", "lastName", "Doe", "email", "jane@example.com", "phone", "+254700000000"),
                EVENT_ID);
    }

    @Test
    void userUpdated_onlyUpsertsSubscriber() {
        handle("UserUpdated", PROFILE);

        verify(novuClient).upsertSubscriber(JANE);
        verify(novuClient, never()).trigger(any(), any(), any(), any());
    }

    @Test
    void userStatusChanged_triggersAccountStatusChanged() {
        handle("UserStatusChanged", ",\"oldStatus\":\"ACTIVE\",\"newStatus\":\"SUSPENDED\"");

        verify(novuClient).trigger(NotificationWorkflow.ACCOUNT_STATUS_CHANGED, USER_ID,
                Map.of("eventId", EVENT_ID, "eventType", "UserStatusChanged", "occurredAt", OCCURRED_AT, "userId", USER_ID,
                        "oldStatus", "ACTIVE", "newStatus", "SUSPENDED"),
                EVENT_ID);
    }

    @Test
    void passwordResetRequested_triggersPasswordReset() {
        handle("PasswordResetRequested", ",\"resetUrl\":\"https://x/reset?t=abc\",\"expiresAt\":\"2026-11-16T11:15:30Z\"");

        verify(novuClient).trigger(NotificationWorkflow.PASSWORD_RESET, USER_ID,
                Map.of("eventId", EVENT_ID, "eventType", "PasswordResetRequested", "occurredAt", OCCURRED_AT, "userId", USER_ID,
                        "resetUrl", "https://x/reset?t=abc", "expiresAt", "2026-11-16T11:15:30Z"),
                EVENT_ID);
    }

    @Test
    void passwordChanged_triggersPasswordChanged() {
        handle("PasswordChanged", "");

        verify(novuClient).trigger(NotificationWorkflow.PASSWORD_CHANGED, USER_ID,
                Map.of("eventId", EVENT_ID, "eventType", "PasswordChanged", "occurredAt", OCCURRED_AT, "userId", USER_ID),
                EVENT_ID);
    }

    @Test
    void missingUserId_isRejected() {
        String json = """
                {"eventId":"%s","eventType":"PasswordChanged","occurredAt":"%s"}
                """.formatted(EVENT_ID, OCCURRED_AT);

        assertThatThrownBy(() -> handler.handle("PasswordChanged", jsonMapper.readTree(json)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId");
        verifyNoInteractions(novuClient);
    }

    @Test
    void unknownEventType_isIgnored() {
        handle("SomethingElse", "");

        verifyNoInteractions(novuClient);
    }
}
