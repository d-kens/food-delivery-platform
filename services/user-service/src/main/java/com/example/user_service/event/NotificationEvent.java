package com.example.user_service.event;

import com.example.user_service.entity.User;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationEvent(UUID eventId, String type, UUID recipientId, Map<String, Object> payload) {

    public static final String UPDATED_PASSWORD = "UPDATED_PASSWORD";

    public static NotificationEvent updatedPassword(User user) {
        return new NotificationEvent(UUID.randomUUID(), UPDATED_PASSWORD, user.getId(),
                Map.of("firstName", user.getFirstName(), "changedAt", Instant.now().toString()));
    }
}
