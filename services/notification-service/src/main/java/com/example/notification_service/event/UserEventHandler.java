package com.example.notification_service.event;

import com.example.notification_service.novu.NotificationWorkflow;
import com.example.notification_service.novu.NovuClient;
import com.example.notification_service.novu.Subscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventHandler {

    private final NovuClient novuClient;
    private final JsonMapper jsonMapper;

    public void handle(String eventType, JsonNode event) {
        switch (eventType) {
            case "UserRegistered" -> {
                upsertSubscriber(event);
                trigger(NotificationWorkflow.WELCOME, event);
            }
            case "UserUpdated" -> upsertSubscriber(event);
            case "UserStatusChanged" -> trigger(NotificationWorkflow.ACCOUNT_STATUS_CHANGED, event);
            case "PasswordResetRequested" -> trigger(NotificationWorkflow.PASSWORD_RESET, event);
            case "PasswordChanged" -> trigger(NotificationWorkflow.PASSWORD_CHANGED, event);
            default -> log.warn("Ignoring unknown user event type {}", eventType);
        }
    }

    private void upsertSubscriber(JsonNode event) {
        novuClient.upsertSubscriber(new Subscriber(
                required(event, "userId"), text(event, "firstName"), text(event, "lastName"),
                text(event, "email"), text(event, "phone")));
    }

    private void trigger(NotificationWorkflow workflow, JsonNode event) {
        Map<String, Object> payload = jsonMapper.convertValue(event, new TypeReference<>() {});
        novuClient.trigger(workflow, required(event, "userId"), payload, event.get("eventId").asString());
    }

    private static String required(JsonNode event, String field) {
        String value = text(event, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Event is missing " + field);
        }
        return value;
    }

    private static String text(JsonNode event, String field) {
        JsonNode node = event.get(field);
        return node == null || node.isNull() ? null : node.asString();
    }
}
