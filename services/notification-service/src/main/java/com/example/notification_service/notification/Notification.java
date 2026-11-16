package com.example.notification_service.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Notification(String eventId, NotificationType type, Recipient recipient, Map<String, Object> data) {

    public Notification {
        if (eventId == null || eventId.isBlank()) {
            throw new InvalidNotificationException("Notification is missing eventId");
        }
        if (type == null) {
            throw new InvalidNotificationException("Notification is missing type");
        }
        if (recipient == null || recipient.id() == null || recipient.id().isBlank()) {
            throw new InvalidNotificationException("Notification is missing recipient.id");
        }
        data = data == null ? Map.of() : data;
    }
}
