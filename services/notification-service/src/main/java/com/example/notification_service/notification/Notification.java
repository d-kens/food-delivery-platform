package com.example.notification_service.notification;

import com.example.notification_service.kafka.InvalidMessageException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Notification(String eventId, NotificationType type, String recipientId, Map<String, Object> payload) {

    public Notification {
        if (eventId == null || eventId.isBlank()) {
            throw new InvalidMessageException("Notification is missing eventId");
        }
        if (type == null) {
            throw new InvalidMessageException("Notification is missing type");
        }
        if (recipientId == null || recipientId.isBlank()) {
            throw new InvalidMessageException("Notification is missing recipientId");
        }
        payload = payload == null ? Map.of() : payload;
    }
}
