package com.example.user_service.event;

import com.example.user_service.entity.User;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationEvent(UUID eventId, String type, UUID recipientId, Map<String, Object> payload) {

    public static final String UPDATED_PASSWORD = "UPDATED_PASSWORD";
    public static final String RESET_PASSWORD = "RESET_PASSWORD";
    public static final String CONFIRM_ACCOUNT_DELETION = "CONFIRM_ACCOUNT_DELETION";
    public static final String CONFIRM_EMAIL_CHANGE = "CONFIRM_EMAIL_CHANGE";

    public static NotificationEvent updatedPassword(User user) {
        return new NotificationEvent(UUID.randomUUID(), UPDATED_PASSWORD, user.getId(),
                Map.of("firstName", user.getFirstName(), "changedAt", Instant.now().toString()));
    }

    public static NotificationEvent passwordReset(User user, String resetUrl, long expiresInMinutes) {
        return new NotificationEvent(UUID.randomUUID(), RESET_PASSWORD, user.getId(), Map.of(
                "firstName", user.getFirstName(),
                "resetUrl", resetUrl,
                "expiresInMinutes", expiresInMinutes));
    }

    public static NotificationEvent confirmAccountDeletion(User user, String confirmUrl, long expiresInMinutes) {
        return new NotificationEvent(UUID.randomUUID(), CONFIRM_ACCOUNT_DELETION, user.getId(), Map.of(
                "firstName", user.getFirstName(),
                "confirmUrl", confirmUrl,
                "expiresInMinutes", expiresInMinutes));
    }

    public static NotificationEvent confirmEmailChange(User user, String newEmail, String confirmUrl, long expiresInMinutes) {
        return new NotificationEvent(UUID.randomUUID(), CONFIRM_EMAIL_CHANGE, user.getId(), Map.of(
                "firstName", user.getFirstName(),
                "newEmail", newEmail,
                "confirmUrl", confirmUrl,
                "expiresInMinutes", expiresInMinutes));
    }
}
